package sprout.security.core;

import java.util.Collection;

public interface Authentication {
    Collection<? extends GrantedAuthority> getAuthorities();
    Object getCredentials();
    Object getPrincipal();
    boolean isAuthenticated();
    void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException;
}
