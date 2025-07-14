package sprout.security.core;

import sprout.security.authentication.exception.UsernameNotFoundException;

public interface UserDetailsService {
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
}
