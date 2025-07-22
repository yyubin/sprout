package sprout.security.authentication;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import sprout.security.authentication.exception.*;
import sprout.security.core.Authentication;
import sprout.security.core.GrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProviderManagerTest {

    // 간단한 Authentication 구현 (필요시 UsernamePasswordAuthenticationToken 써도 됨)
    static class DummyAuth implements Authentication {
        private boolean auth;
        @Override public List<? extends GrantedAuthority> getAuthorities() { return List.of(); }
        @Override public Object getCredentials() { return "pw"; }
        @Override public Object getPrincipal() { return "user"; }
        @Override public boolean isAuthenticated() { return auth; }
        @Override public void setAuthenticated(boolean isAuthenticated) { this.auth = isAuthenticated; }
    }

    @Test
    @DisplayName("첫 번째 지원 Provider가 성공하면 즉시 반환하고 성공 이벤트를 발행한다")
    void successOnFirstProvider() throws Exception {
        AuthenticationEventPublisher publisher = mock(AuthenticationEventPublisher.class);
        AuthenticationProvider p1 = mock(AuthenticationProvider.class);
        AuthenticationProvider p2 = mock(AuthenticationProvider.class);
        Authentication input = new DummyAuth();
        Authentication output = new DummyAuth();

        when(p1.supports(input.getClass())).thenReturn(true);
        when(p1.authenticate(input)).thenReturn(output);

        ProviderManager manager = new ProviderManager(publisher, List.of(p1, p2), null);

        Authentication result = manager.authenticate(input);
        assertSame(output, result);

        verify(publisher).publishAuthenticationSuccess(output);
        verify(publisher, never()).publishAuthenticationFailure(any(), any());
        verify(p2, never()).authenticate(any());
    }

    @Test
    @DisplayName("첫 Provider가 BadCredentials 등 일반 실패를 던지고 두 번째가 성공하면 성공 이벤트 발행 후 반환")
    void firstFailsSecondSucceeds() throws Exception {
        AuthenticationEventPublisher publisher = mock(AuthenticationEventPublisher.class);
        AuthenticationProvider bad = mock(AuthenticationProvider.class);
        AuthenticationProvider good = mock(AuthenticationProvider.class);
        Authentication input = new DummyAuth();
        Authentication output = new DummyAuth();

        when(bad.supports(input.getClass())).thenReturn(true);
        when(bad.authenticate(input)).thenThrow(new BadCredentialsException("bad pwd"));
        when(good.supports(input.getClass())).thenReturn(true);
        when(good.authenticate(input)).thenReturn(output);

        ProviderManager manager = new ProviderManager(publisher, List.of(bad, good), null);

        Authentication result = manager.authenticate(input);
        assertSame(output, result);

        // 실패 이벤트는 마지막에만 치는 정책이라 good 성공 시 실패 이벤트 없음
        verify(publisher).publishAuthenticationSuccess(output);
        verify(publisher, never()).publishAuthenticationFailure(any(), any());
    }

    @Test
    @DisplayName("AccountExpired/CredentialExpired 같은 치명적 예외는 즉시 던지고 실패 이벤트 발행")
    void immediateFatalException() throws Exception {
        AuthenticationEventPublisher publisher = mock(AuthenticationEventPublisher.class);
        AuthenticationProvider p1 = mock(AuthenticationProvider.class);
        Authentication input = new DummyAuth();

        when(p1.supports(input.getClass())).thenReturn(true);
        when(p1.authenticate(input)).thenThrow(new AccountExpiredException("expired"));

        ProviderManager manager = new ProviderManager(publisher, List.of(p1), null);

        AccountExpiredException ex = assertThrows(AccountExpiredException.class,
                () -> manager.authenticate(input));

        verify(publisher).publishAuthenticationFailure(ex, input);
        verify(publisher, never()).publishAuthenticationSuccess(any());
    }

    @Test
    @DisplayName("지원 Provider가 없고 부모도 없으면 ProviderNotFoundException")
    void noProviderAndNoParent() {
        AuthenticationEventPublisher publisher = mock(AuthenticationEventPublisher.class);
        AuthenticationProvider p = mock(AuthenticationProvider.class);
        Authentication input = new DummyAuth();

        when(p.supports(any())).thenReturn(false);

        ProviderManager manager = new ProviderManager(publisher, List.of(p), null);

        ProviderNotFoundException ex = assertThrows(ProviderNotFoundException.class,
                () -> manager.authenticate(input));

        // lastException == null 경로이므로 failure 이벤트 발행 안 됨
        verify(publisher, never()).publishAuthenticationFailure(any(), any());
        verify(publisher, never()).publishAuthenticationSuccess(any());
    }

    @Test
    @DisplayName("현재 Provider들은 실패해도 Parent가 성공하면 성공 이벤트 발행 후 반환")
    void parentSucceeds() throws Exception {
        AuthenticationEventPublisher publisher = mock(AuthenticationEventPublisher.class);
        AuthenticationProvider p1 = mock(AuthenticationProvider.class);
        AuthenticationManager parent = mock(AuthenticationManager.class);
        Authentication input = new DummyAuth();
        Authentication parentOut = new DummyAuth();

        when(p1.supports(input.getClass())).thenReturn(true);
        when(p1.authenticate(input)).thenThrow(new BadCredentialsException("bad"));

        when(parent.authenticate(input)).thenReturn(parentOut);

        ProviderManager manager = new ProviderManager(publisher, List.of(p1), parent);

        Authentication result = manager.authenticate(input);
        assertSame(parentOut, result);

        verify(publisher).publishAuthenticationSuccess(parentOut);
        verify(publisher, never()).publishAuthenticationFailure(any(), any());
    }

    @Test
    @DisplayName("현재 Provider 실패 후 Parent도 LoginException 던지면 마지막 예외를 던지고 실패 이벤트 발행")
    void parentFailsToo() throws Exception {
        AuthenticationEventPublisher publisher = mock(AuthenticationEventPublisher.class);
        AuthenticationProvider p1 = mock(AuthenticationProvider.class);
        AuthenticationManager parent = mock(AuthenticationManager.class);
        Authentication input = new DummyAuth();
        BadCredentialsException last = new BadCredentialsException("bad again");

        when(p1.supports(input.getClass())).thenReturn(true);
        when(p1.authenticate(input)).thenThrow(new BadCredentialsException("bad"));

        when(parent.authenticate(input)).thenThrow(last);

        ProviderManager manager = new ProviderManager(publisher, List.of(p1), parent);

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> manager.authenticate(input));
        assertSame(last, ex);

        verify(publisher).publishAuthenticationFailure(last, input);
        verify(publisher, never()).publishAuthenticationSuccess(any());
    }

    @Test
    @DisplayName("Provider가 런타임 예외를 던지면 AuthenticationException으로 감싸서 실패 이벤트 발행 후 던진다")
    void runtimeWrapped() throws Exception {
        AuthenticationEventPublisher publisher = mock(AuthenticationEventPublisher.class);
        AuthenticationProvider p1 = mock(AuthenticationProvider.class);
        Authentication input = new DummyAuth();

        when(p1.supports(input.getClass())).thenReturn(true);
        when(p1.authenticate(input)).thenAnswer(inv -> { throw new RuntimeException("boom"); });

        ProviderManager manager = new ProviderManager(publisher, List.of(p1), null);

        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> manager.authenticate(input));

        assertTrue(ex.getMessage().contains("Internal authentication service error"));
        verify(publisher).publishAuthenticationFailure(ex, input);
        verify(publisher, never()).publishAuthenticationSuccess(any());
    }

    @Test
    @DisplayName("이벤트 순서: 실패 후 성공 시 성공만, 실패 최종시 실패만")
    void eventOrder() throws Exception {
        AuthenticationEventPublisher publisher = mock(AuthenticationEventPublisher.class);
        AuthenticationProvider bad = mock(AuthenticationProvider.class);
        AuthenticationProvider good = mock(AuthenticationProvider.class);
        Authentication input = new DummyAuth();
        Authentication out = new DummyAuth();

        when(bad.supports(input.getClass())).thenReturn(true);
        when(bad.authenticate(input)).thenThrow(new BadCredentialsException("bad"));
        when(good.supports(input.getClass())).thenReturn(true);
        when(good.authenticate(input)).thenReturn(out);

        ProviderManager manager = new ProviderManager(publisher, List.of(bad, good), null);
        manager.authenticate(input);

        InOrder inOrder = inOrder(publisher);
        inOrder.verify(publisher).publishAuthenticationSuccess(out);
        inOrder.verifyNoMoreInteractions();
    }
}
