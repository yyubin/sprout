package sprout.security.core;

import java.util.Collection;
import java.util.List;

public class DefaultUserDetails implements UserDetails{

    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private boolean authenticated;
    private boolean isExpired;
    private boolean isLocked;
    private boolean isCredentialsExpired;
    private boolean isEnabled;

    public DefaultUserDetails(String username, String password, Collection<? extends GrantedAuthority> authorities) {
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.authenticated = false;
    }

    public DefaultUserDetails(String username, String password, Collection<? extends GrantedAuthority> authorities, boolean isExpired, boolean isLocked, boolean isCredentialsExpired, boolean isEnabled) {
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.authenticated = false;
        this.isExpired = isExpired;
        this.isLocked = isLocked;
        this.isCredentialsExpired = isCredentialsExpired;
        this.isEnabled = isEnabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return this.isExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.isLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.isCredentialsExpired;
    }

    @Override
    public boolean isEnabled() {
        return this.isEnabled;
    }
}
