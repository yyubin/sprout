package sprout.security.filter;

import com.fasterxml.jackson.core.JsonParseException;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import sprout.core.filter.FilterChain;
import sprout.security.authentication.AuthenticationManager;
import sprout.security.authentication.UsernamePasswordAuthenticationToken;
import sprout.security.authentication.exception.BadCredentialsException;
import sprout.security.context.*;
import sprout.security.core.Authentication;
import sprout.security.web.util.matcher.RequestMatcher;
import sprout.mvc.http.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationFilterTest {

    AuthenticationFilter filter;
    AuthenticationManager authManager;
    RequestMatcher matcher;     // 매치되는 하나
    RequestMatcher noMatch;     // 매치되지 않는 하나

    @BeforeEach
    void setUp() throws Exception {
        authManager = mock(AuthenticationManager.class);
        matcher = mock(RequestMatcher.class);
        noMatch = mock(RequestMatcher.class);

        filter = new AuthenticationFilter(List.of(noMatch, matcher), authManager);

        // SecurityContextHolder.strategy 세팅 (ThreadLocal)
        Field f = SecurityContextHolder.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(null, new ThreadLocalSecurityContextHolderStrategy());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ---- helpers -------------------------------------------------------------

    private HttpRequest req(String path, HttpMethod method, Object body) {
        HttpRequest r = mock(HttpRequest.class);
        when(r.getPath()).thenReturn(path);
        when(r.getMethod()).thenReturn(method);
        when(r.getBody()).thenReturn(body);
        return r;
    }

    private HttpResponse res() {
        return mock(HttpResponse.class);
    }

    private FilterChain chain() throws java.io.IOException {
        FilterChain chain = mock(FilterChain.class);
        doNothing().when(chain).doFilter(any(), any());
        return chain;
    }

    private ResponseEntity<?> capturedEntity(HttpResponse res) {
        ArgumentCaptor<ResponseEntity> captor = ArgumentCaptor.forClass(ResponseEntity.class);
        verify(res).setResponseEntity(captor.capture());
        return captor.getValue();
    }

    // ---- tests --------------------------------------------------------------

    @Test
    @DisplayName("매처 불일치 시 체인으로 그대로 진행")
    void notMatched_chainContinues() throws Exception {
        HttpRequest req = req("/other", HttpMethod.GET, null);
        HttpResponse res = res();
        FilterChain chain = chain();

        when(noMatch.matches(req)).thenReturn(false);
        when(matcher.matches(req)).thenReturn(false);

        filter.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
        verify(res, never()).setResponseEntity(any());
    }

    @Test
    @DisplayName("POST가 아니면 405 반환")
    void methodNotAllowed() throws Exception {
        HttpRequest req = req("/login", HttpMethod.GET, "{}");
        HttpResponse res = res();
        FilterChain chain = chain();

        when(noMatch.matches(req)).thenReturn(false);
        when(matcher.matches(req)).thenReturn(true);

        filter.doFilter(req, res, chain);

        ResponseEntity<?> entity = capturedEntity(res);
        assertEquals(ResponseCode.METHOD_NOT_ALLOWED, entity.getStatusCode());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("JSON 파싱 실패 → 400")
    void badJson() throws Exception {
        HttpRequest req = req("/login", HttpMethod.POST, "not-a-json");
        HttpResponse res = res();
        FilterChain chain = chain();

        when(noMatch.matches(req)).thenReturn(false);
        when(matcher.matches(req)).thenReturn(true);

        filter.doFilter(req, res, chain);

        ResponseEntity<?> entity = capturedEntity(res);
        assertEquals(ResponseCode.BAD_REQUEST, entity.getStatusCode());
        assertTrue(entity.getBody().toString().contains("Invalid request body"));
    }

    @Test
    @DisplayName("username 누락 → 401")
    void missingUsername() throws Exception {
        HttpRequest req = req("/login", HttpMethod.POST, "{\"password\":\"p\"}");
        HttpResponse res = res();
        FilterChain chain = chain();

        when(noMatch.matches(req)).thenReturn(false);
        when(matcher.matches(req)).thenReturn(true);

        filter.doFilter(req, res, chain);

        ResponseEntity<?> entity = capturedEntity(res);
        assertEquals(ResponseCode.UNAUTHORIZED, entity.getStatusCode());
        assertTrue(entity.getBody().toString().contains("Username not provided"));
    }

    @Test
    @DisplayName("password 누락 → 401")
    void missingPassword() throws Exception {
        HttpRequest req = req("/login", HttpMethod.POST, "{\"username\":\"u\"}");
        HttpResponse res = res();
        FilterChain chain = chain();

        when(noMatch.matches(req)).thenReturn(false);
        when(matcher.matches(req)).thenReturn(true);

        filter.doFilter(req, res, chain);

        ResponseEntity<?> entity = capturedEntity(res);
        assertEquals(ResponseCode.UNAUTHORIZED, entity.getStatusCode());
        assertTrue(entity.getBody().toString().contains("Password not provided"));
    }

    @Test
    @DisplayName("AuthenticationManager 인증 실패(LoginException) → 401")
    void loginException() throws Exception {
        HttpRequest req = req("/login", HttpMethod.POST, "{\"username\":\"u\",\"password\":\"p\"}");
        HttpResponse res = res();
        FilterChain chain = chain();

        when(noMatch.matches(req)).thenReturn(false);
        when(matcher.matches(req)).thenReturn(true);
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad"));

        filter.doFilter(req, res, chain);

        ResponseEntity<?> entity = capturedEntity(res);
        assertEquals(ResponseCode.UNAUTHORIZED, entity.getStatusCode());
        assertTrue(entity.getBody().toString().contains("Authentication failed"));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("성공 시 SecurityContextHolder에 저장하고 200 반환")
    void success() throws Exception {
        HttpRequest req = req("/login", HttpMethod.POST, "{\"username\":\"u\",\"password\":\"p\"}");
        HttpResponse res = res();
        FilterChain chain = chain();

        Authentication authenticated = mock(Authentication.class);
        when(noMatch.matches(req)).thenReturn(false);
        when(matcher.matches(req)).thenReturn(true);
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authenticated);

        filter.doFilter(req, res, chain);

        ResponseEntity<?> entity = capturedEntity(res);
        assertEquals(ResponseCode.SUCCESS, entity.getStatusCode());
        assertSame(authenticated, SecurityContextHolder.getContext().getAuthentication());
        verify(chain, never()).doFilter(any(), any()); // 로그인 요청이면 체인 진행 안 함
    }

    @Test
    @DisplayName("예기치 못한 Exception → 400")
    void unexpectedException() throws Exception {
        HttpRequest req = req("/login", HttpMethod.POST, "{\"username\":\"u\",\"password\":\"p\"}");
        HttpResponse res = res();
        FilterChain chain = chain();

        when(noMatch.matches(req)).thenReturn(false);
        when(matcher.matches(req)).thenReturn(true);
        when(authManager.authenticate(any())).thenAnswer(inv -> { throw new RuntimeException("boom"); });

        filter.doFilter(req, res, chain);

        ResponseEntity<?> entity = capturedEntity(res);
        assertEquals(ResponseCode.BAD_REQUEST, entity.getStatusCode());
    }
}
