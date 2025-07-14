package sprout.security.authentication;

import sprout.security.core.Authentication;
import sprout.security.core.GrantedAuthority;
import sprout.security.core.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class UsernamePasswordAuthenticationToken implements Authentication {

    private final Object principal;
    private final Object credentials;
    private final Collection<? extends GrantedAuthority> authorities;
    private boolean authenticated;

    public UsernamePasswordAuthenticationToken(Object principal, Object credentials) {
        this.principal = principal;
        this.credentials = credentials;
        this.authorities = Collections.emptySet();
        this.authenticated = false;
    }

    public UsernamePasswordAuthenticationToken(UserDetails principal, Object credentials,
                                               Collection<? extends GrantedAuthority> authorities) {
        this.principal = principal;
        this.credentials = credentials; // 인증 후에는 보통 null로 설정
        this.authorities = Collections.unmodifiableSet(new HashSet<>(authorities));
        this.authenticated = true;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public Object getCredentials() {
        return this.credentials;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }

    @Override
    public boolean isAuthenticated() {
        return this.authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        if (isAuthenticated) {
            throw new IllegalArgumentException("Cannot set this token to authenticated. Use a constructor that takes a GrantedAuthority list.");
        }
        this.authenticated = false;
    }

}
