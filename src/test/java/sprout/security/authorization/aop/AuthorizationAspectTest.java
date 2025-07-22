package sprout.security.authorization.aop;

import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import sprout.aop.JoinPoint;
import sprout.security.authorization.annotation.PreAuthorize;
import sprout.security.context.*;
import sprout.security.core.Authentication;
import sprout.security.core.GrantedAuthority;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.AccessDeniedException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthorizationAspectTest {

    AuthorizationAspect aspect;

    @BeforeEach
    void setUp() throws Exception {
        aspect = new AuthorizationAspect();
        // SecurityContextHolder.strategy 교체 (ThreadLocal)
        Field f = SecurityContextHolder.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(null, new ThreadLocalSecurityContextHolderStrategy());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ----- helper -----
    private Authentication auth(String principal, String... roles) {
        Authentication a = mock(Authentication.class);
        when(a.getPrincipal()).thenReturn(principal);
        when(a.isAuthenticated()).thenReturn(true);
        List<GrantedAuthority> list = java.util.Arrays.stream(roles)
                .map(r -> (GrantedAuthority) () -> r)
                .toList();
        doReturn(list).when(a).getAuthorities();
        return a;
    }

    private JoinPoint jpFor(Method m) {
        JoinPoint jp = mock(JoinPoint.class);
        when(jp.getMethod()).thenReturn(m);
        return jp;
    }

    // ----- target class for annotation -----
    static class Target {

        @PreAuthorize("ADMIN")
        public void adminOnly() {}

        @PreAuthorize("") // blank -> 경고만, 통과
        public void blankValue() {}

        public void noAnno() {}
    }

    @Test
    @DisplayName("필요 권한이 있고, 사용자에게 권한이 있으면 통과")
    void hasAuthority_pass() throws Exception {
        Method m = Target.class.getMethod("adminOnly");
        JoinPoint jp = jpFor(m);

        SecurityContextHolder.setContext(new SecurityContextImpl(auth("alice", "ADMIN")));

        assertDoesNotThrow(() -> aspect.preAuthorize(jp));
    }

    @Test
    @DisplayName("필요 권한이 있는데 사용자에게 없으면 AccessDeniedException")
    void missingAuthority_denied() throws Exception {
        Method m = Target.class.getMethod("adminOnly");
        JoinPoint jp = jpFor(m);

        SecurityContextHolder.setContext(new SecurityContextImpl(auth("bob", "USER")));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> aspect.preAuthorize(jp));
        assertTrue(ex.getMessage().contains("bob"));
        assertTrue(ex.getMessage().contains("adminOnly"));
    }

    @Test
    @DisplayName("Authentication 자체가 없으면 권한 없음으로 간주 → AccessDeniedException")
    void noAuthentication_denied() throws Exception {
        Method m = Target.class.getMethod("adminOnly");
        JoinPoint jp = jpFor(m);

        // SecurityContext 비어있음
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());

        assertThrows(AccessDeniedException.class, () -> aspect.preAuthorize(jp));
    }

    @Test
    @DisplayName("@PreAuthorize value가 빈 문자열이면 검사하지 않고 통과")
    void blankValue_pass() throws Exception {
        Method m = Target.class.getMethod("blankValue");
        JoinPoint jp = jpFor(m);

        // 아무 권한 없어도 통과
        SecurityContextHolder.setContext(new SecurityContextImpl(auth("charlie")));

        assertDoesNotThrow(() -> aspect.preAuthorize(jp));
    }

    @Test
    @DisplayName("@PreAuthorize 가 없으면 아무 것도 안 함")
    void noAnnotation_doNothing() throws Exception {
        Method m = Target.class.getMethod("noAnno");
        JoinPoint jp = jpFor(m);

        // auth 없어도 예외 없음
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());

        assertDoesNotThrow(() -> aspect.preAuthorize(jp));
    }
}
