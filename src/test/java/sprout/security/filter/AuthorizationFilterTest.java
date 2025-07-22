package sprout.security.filter;

import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import sprout.core.filter.FilterChain;
import sprout.mvc.http.*;
import sprout.security.authorization.AuthorizationRule;
import sprout.security.authorization.exception.AccessDeniedException;
import sprout.security.context.*;
import sprout.security.core.Authentication;
import sprout.security.core.GrantedAuthority;
import sprout.security.web.util.matcher.RequestMatcher;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthorizationFilterTest {

    // ---------- 공통 준비/정리 ----------
    @BeforeEach
    void initHolder() throws Exception {
        Field f = SecurityContextHolder.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(null, new ThreadLocalSecurityContextHolderStrategy());
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // ---------- helpers ----------
    private HttpRequest req(String path, HttpMethod m) {
        HttpRequest r = mock(HttpRequest.class);
        when(r.getPath()).thenReturn(path);
        when(r.getMethod()).thenReturn(m);
        return r;
    }

    private HttpResponse res() {
        return mock(HttpResponse.class);
    }

    private FilterChain chain() throws java.io.IOException {
        FilterChain c = mock(FilterChain.class);
        doNothing().when(c).doFilter(any(), any());
        return c;
    }

    private RequestMatcher matcher(boolean matches) {
        RequestMatcher m = mock(RequestMatcher.class);
        when(m.matches(any())).thenReturn(matches);
        return m;
    }

    private Authentication auth(boolean isAuth, String principal, String... auths) {
        Authentication a = mock(Authentication.class);
        when(a.isAuthenticated()).thenReturn(isAuth);
        when(a.getPrincipal()).thenReturn(principal);
        List<GrantedAuthority> list = java.util.Arrays.stream(auths)
                .map(s -> (GrantedAuthority) () -> s).toList();
        doReturn(list).when(a).getAuthorities();
        return a;
    }

    private ResponseEntity<?> capturedEntity(HttpResponse res) {
        var cap = ArgumentCaptor.forClass(ResponseEntity.class);
        verify(res).setResponseEntity(cap.capture());
        return cap.getValue();
    }

    // ---------- tests ----------

    @Test
    @DisplayName("매칭 규칙 없음 → 체인 통과")
    void noRule_passThrough() throws Exception {
        AuthorizationFilter filter = new AuthorizationFilter(List.of());

        HttpRequest req = req("/x", HttpMethod.GET);
        HttpResponse res = res();
        FilterChain chain = chain();

        filter.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
        verify(res, never()).setResponseEntity(any());
    }

    @Test
    @DisplayName("permitAll 규칙 → 인증 없이 통과")
    void permitAll() throws Exception {
        RequestMatcher mTrue = matcher(true);
        AuthorizationRule rule = AuthorizationRule.permitAll(mTrue);
        AuthorizationFilter filter = new AuthorizationFilter(List.of(rule));

        HttpRequest req = req("/open", HttpMethod.GET);
        HttpResponse res = res();
        FilterChain chain = chain();

        filter.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
        verify(res, never()).setResponseEntity(any());
    }

    @Test
    @DisplayName("인증 필요 규칙인데 인증 없음 → 401")
    void needAuth_butNoAuth() throws Exception {
        RequestMatcher mTrue = matcher(true);
        AuthorizationRule rule = AuthorizationRule.authenticated(mTrue);
        AuthorizationFilter filter = new AuthorizationFilter(List.of(rule));

        HttpRequest req = req("/secure", HttpMethod.GET);
        HttpResponse res = res();
        FilterChain chain = chain();

        filter.doFilter(req, res, chain);

        ResponseEntity<?> entity = capturedEntity(res);
        assertEquals(ResponseCode.UNAUTHORIZED, entity.getStatusCode());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("authenticated 규칙 → 인증되어 있으면 통과")
    void authenticatedRule_pass() throws Exception {
        RequestMatcher mTrue = matcher(true);
        AuthorizationRule rule = AuthorizationRule.authenticated(mTrue);
        AuthorizationFilter filter = new AuthorizationFilter(List.of(rule));

        Authentication a = auth(true, "john");
        SecurityContextHolder.setContext(new SecurityContextImpl(a));

        HttpRequest req = req("/me", HttpMethod.GET);
        HttpResponse res = res();
        FilterChain chain = chain();

        filter.doFilter(req, res, chain);
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("권한 요구 규칙: 하나라도 갖고 있으면 통과")
    void authorityMatch_pass() throws Exception {
        RequestMatcher mTrue = matcher(true);
        AuthorizationRule rule = AuthorizationRule.hasAnyAuthority(mTrue, "ADMIN", "USER");
        AuthorizationFilter filter = new AuthorizationFilter(List.of(rule));

        Authentication a = auth(true, "jane", "USER");
        SecurityContextHolder.setContext(new SecurityContextImpl(a));

        HttpRequest req = req("/admin", HttpMethod.GET);
        HttpResponse res = res();
        FilterChain chain = chain();

        filter.doFilter(req, res, chain);
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("권한 요구 규칙: 아무 권한도 없으면 AccessDeniedException")
    void authorityMismatch_denied() throws Exception {
        RequestMatcher mTrue = matcher(true);
        AuthorizationRule rule = AuthorizationRule.hasAnyAuthority(mTrue, "ADMIN");
        AuthorizationFilter filter = new AuthorizationFilter(List.of(rule));

        Authentication a = auth(true, "bob", "USER");
        SecurityContextHolder.setContext(new SecurityContextImpl(a));

        HttpRequest req = req("/admin", HttpMethod.GET);
        HttpResponse res = res();
        FilterChain chain = chain();

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> filter.doFilter(req, res, chain));
        assertTrue(ex.getMessage().contains("bob"));
        verify(res, never()).setResponseEntity(any());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("조건이 아무 것도 안 맞는 규칙 → 방어적 AccessDeniedException")
    void defensive_denied() throws Exception {
        RequestMatcher mTrue = matcher(true);
        // hasAnyAuthority 에 아무 것도 주지 않으면 empty set → permitAll/ authenticated 도 false
        AuthorizationRule weird = AuthorizationRule.hasAnyAuthority(mTrue);
        AuthorizationFilter filter = new AuthorizationFilter(List.of(weird));

        Authentication a = auth(true, "x");
        SecurityContextHolder.setContext(new SecurityContextImpl(a));

        HttpRequest req = req("/weird", HttpMethod.GET);
        HttpResponse res = res();
        FilterChain chain = chain();

        assertThrows(AccessDeniedException.class,
                () -> filter.doFilter(req, res, chain));
    }

    @Test
    @DisplayName("여러 규칙 중 첫 번째 매칭만 사용")
    void firstMatchOnly() throws Exception {
        RequestMatcher mTrue = matcher(true);
        AuthorizationRule first = AuthorizationRule.permitAll(mTrue);
        AuthorizationRule second = AuthorizationRule.authenticated(mTrue); // 무시될 예정
        AuthorizationFilter filter = new AuthorizationFilter(List.of(first, second));

        HttpRequest req = req("/any", HttpMethod.GET);
        HttpResponse res = res();
        FilterChain chain = chain();

        filter.doFilter(req, res, chain);
        verify(chain).doFilter(req, res);
    }
}
