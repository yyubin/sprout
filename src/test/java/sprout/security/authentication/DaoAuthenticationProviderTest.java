package sprout.security.authentication;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sprout.security.authentication.exception.*;
import sprout.security.authentication.password.PasswordEncoder;
import sprout.security.core.Authentication;
import sprout.security.core.GrantedAuthority;
import sprout.security.core.UserDetails;
import sprout.security.core.UserDetailsService;

import javax.naming.AuthenticationException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DaoAuthenticationProviderTest {

    UserDetailsService uds = mock(UserDetailsService.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(uds, encoder);

    private UsernamePasswordAuthenticationToken unauth(String u, String p) {
        return new UsernamePasswordAuthenticationToken(u, p);
    }

    private UserDetails stubUser(String username, String encodedPw,
                                 boolean nonExpired, boolean nonLocked,
                                 boolean credNonExpired, boolean enabled,
                                 List<? extends GrantedAuthority> auths) {

        UserDetails ud = mock(UserDetails.class);
        when(ud.getUsername()).thenReturn(username);
        when(ud.getPassword()).thenReturn(encodedPw);
        when(ud.isAccountNonExpired()).thenReturn(nonExpired);
        when(ud.isAccountNonLocked()).thenReturn(nonLocked);
        when(ud.isCredentialsNonExpired()).thenReturn(credNonExpired);
        when(ud.isEnabled()).thenReturn(enabled);
        doReturn(auths).when(ud).getAuthorities();
        return ud;
    }

    @Test
    @DisplayName("supports는 UsernamePasswordAuthenticationToken에만 true")
    void supports() {
        assertTrue(provider.supports(UsernamePasswordAuthenticationToken.class));
        assertFalse(provider.supports(Authentication.class)); // 임의의 다른 타입
    }

    @Test
    @DisplayName("정상 인증: 비밀번호 일치 & 모든 상태 OK → 인증 토큰 반환")
    void authenticate_success() throws AuthenticationException {
        UsernamePasswordAuthenticationToken input = unauth("john", "raw");
        UserDetails user = stubUser("john", "enc", true, true, true, true, List.of());

        when(uds.loadUserByUsername("john")).thenReturn(user);
        when(encoder.matches("raw", "enc")).thenReturn(true);

        Authentication result = provider.authenticate(input);

        assertNotNull(result);
        assertTrue(result instanceof UsernamePasswordAuthenticationToken);
        assertTrue(result.isAuthenticated());
        assertSame(user, result.getPrincipal());
        assertNull(result.getCredentials()); // 생성자에서 null 넣었는지 확인
    }

    @Test
    @DisplayName("사용자 없음 → BadCredentialsException(wrapper of UsernameNotFoundException)")
    void userNotFound() {
        UsernamePasswordAuthenticationToken input = unauth("nope", "pw");
        when(uds.loadUserByUsername("nope")).thenThrow(new UsernameNotFoundException("404"));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> provider.authenticate(input));
        assertTrue(ex.getCause() instanceof UsernameNotFoundException);
    }

    @Test
    @DisplayName("비밀번호 불일치 → BadCredentialsException")
    void badPassword() throws AuthenticationException {
        UsernamePasswordAuthenticationToken input = unauth("john", "wrong");
        UserDetails user = stubUser("john", "enc", true, true, true, true, List.of());
        when(uds.loadUserByUsername("john")).thenReturn(user);
        when(encoder.matches("wrong", "enc")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> provider.authenticate(input));
    }

    @Nested
    @DisplayName("계정 상태 예외들")
    class AccountState {

        @Test
        @DisplayName("만료 → AccountExpiredException")
        void expired() throws AuthenticationException {
            UsernamePasswordAuthenticationToken input = unauth("u", "p");
            UserDetails user = stubUser("u", "e", false, true, true, true, List.of());
            when(uds.loadUserByUsername("u")).thenReturn(user);
            when(encoder.matches("p", "e")).thenReturn(true);

            assertThrows(AccountExpiredException.class, () -> provider.authenticate(input));
        }

        @Test
        @DisplayName("락 → AccountExpiredException (locked 분기)")
        void locked() throws AuthenticationException {
            UsernamePasswordAuthenticationToken input = unauth("u", "p");
            UserDetails user = stubUser("u", "e", true, false, true, true, List.of());
            when(uds.loadUserByUsername("u")).thenReturn(user);
            when(encoder.matches("p", "e")).thenReturn(true);

            assertThrows(AccountExpiredException.class, () -> provider.authenticate(input));
        }

        @Test
        @DisplayName("자격증명 만료 → CredentialExpiredException")
        void credentialExpired() throws AuthenticationException {
            UsernamePasswordAuthenticationToken input = unauth("u", "p");
            UserDetails user = stubUser("u", "e", true, true, false, true, List.of());
            when(uds.loadUserByUsername("u")).thenReturn(user);
            when(encoder.matches("p", "e")).thenReturn(true);

            assertThrows(CredentialExpiredException.class, () -> provider.authenticate(input));
        }

        @Test
        @DisplayName("비활성 → AccountExpiredException (disabled 분기)")
        void disabled() throws AuthenticationException {
            UsernamePasswordAuthenticationToken input = unauth("u", "p");
            UserDetails user = stubUser("u", "e", true, true, true, false, List.of());
            when(uds.loadUserByUsername("u")).thenReturn(user);
            when(encoder.matches("p", "e")).thenReturn(true);

            assertThrows(AccountExpiredException.class, () -> provider.authenticate(input));
        }
    }
}
