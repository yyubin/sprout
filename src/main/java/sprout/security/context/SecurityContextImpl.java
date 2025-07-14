package sprout.security.context;

import sprout.security.core.Authentication;
import sprout.security.core.SecurityContext;

public class SecurityContextImpl implements SecurityContext {

    private final Authentication authentication;

    public SecurityContextImpl(Authentication authentication) {
        this.authentication = authentication;
    }

    @Override
    public Authentication getAuthentication() {
        return this.authentication;
    }

}
