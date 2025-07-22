package sprout.security.authentication;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sprout.config.AppConfig;
import sprout.security.authentication.exception.UsernameNotFoundException;
import sprout.security.authentication.password.PasswordEncoder;
import sprout.security.core.UserDetails;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DefaultUserDetailsServiceTest {

    @Test
    @DisplayName("username='user' 이면 AppConfig 비밀번호를 인코딩해 UserDetails를 반환한다")
    void loadUser_success() {
        AppConfig config = mock(AppConfig.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);

        // given
        when(config.getStringProperty("sprout.security.default.user.password", "password"))
                .thenReturn("plainPW");
        when(encoder.encode("plainPW")).thenReturn("ENC_plainPW");

        DefaultUserDetailsService service = new DefaultUserDetailsService(config, encoder);

        // when
        UserDetails details = service.loadUserByUsername("user");

        // then
        assertEquals("user", details.getUsername());
        assertEquals("ENC_plainPW", details.getPassword());
        assertEquals(1, details.getAuthorities().size());
        assertEquals("USER", details.getAuthorities().iterator().next().getAuthority());

        verify(encoder).encode("plainPW");
        verify(config).getStringProperty("sprout.security.default.user.password", "password");
    }

    @Test
    @DisplayName("존재하지 않는 사용자면 UsernameNotFoundException을 던진다")
    void loadUser_notFound() {
        AppConfig config = mock(AppConfig.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        DefaultUserDetailsService service = new DefaultUserDetailsService(config, encoder);

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("nobody"));
    }
}
