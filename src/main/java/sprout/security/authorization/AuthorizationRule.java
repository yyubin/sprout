package sprout.security.authorization;

import sprout.security.web.util.matcher.RequestMatcher;

import java.util.Collections;
import java.util.Set;

public class AuthorizationRule {
    private final RequestMatcher requestMatcher;
    private final Set<String> requiredAuthorities;
    private final boolean permitAll;
    private final boolean authenticated;

    public static AuthorizationRule permitAll(RequestMatcher matcher) {
        return new AuthorizationRule(matcher, Collections.emptySet(), true, false);
    }

    // 인증된 사용자만 허용 (authenticated)
    public static AuthorizationRule authenticated(RequestMatcher matcher) {
        return new AuthorizationRule(matcher, Collections.emptySet(), false, true);
    }

    // 특정 권한 필요 (hasAnyAuthority)
    public static AuthorizationRule hasAnyAuthority(RequestMatcher matcher, String... authorities) {
        return new AuthorizationRule(matcher, Set.of(authorities), false, false);
    }

    // private 생성자
    private AuthorizationRule(RequestMatcher requestMatcher, Set<String> requiredAuthorities, boolean permitAll, boolean authenticated) {
        this.requestMatcher = requestMatcher;
        this.requiredAuthorities = requiredAuthorities;
        this.permitAll = permitAll;
        this.authenticated = authenticated;
    }

    public RequestMatcher getRequestMatcher() { return requestMatcher; }
    public Set<String> getRequiredAuthorities() { return requiredAuthorities; }
    public boolean isPermitAll() { return permitAll; }
    public boolean isAuthenticated() { return authenticated; }

}
