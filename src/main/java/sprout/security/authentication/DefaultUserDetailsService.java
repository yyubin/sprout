package sprout.security.authentication;

import sprout.config.AppConfig;
import sprout.security.authentication.exception.UsernameNotFoundException;
import sprout.security.authentication.password.BCryptPasswordEncoder;
import sprout.security.authentication.password.PasswordEncoder;
import sprout.security.core.*;

import java.util.List;

public class DefaultUserDetailsService implements UserDetailsService {

    private final AppConfig appConfig;
    private final PasswordEncoder passwordEncoder;

    public DefaultUserDetailsService(AppConfig appConfig, PasswordEncoder passwordEncoder) {
        this.appConfig = appConfig;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if ("user".equals(username)) {
            String defaultRawPassword = appConfig.getStringProperty("sprout.security.default.user.password", "password");
            String encodedPassword = passwordEncoder.encode(defaultRawPassword);
            GrantedAuthority grantedAuthority = new DefaultGrantedAuthority("USER");
            return new DefaultUserDetails(username, encodedPassword, List.of(grantedAuthority));
        }
        throw new UsernameNotFoundException("Default UserDetailsService: User '" + username + "' not found.");
    }
}
