package sprout.security.filter;

import sprout.beans.InfrastructureBean;
import sprout.core.filter.Filter;
import sprout.core.filter.FilterChain;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.HttpResponse;
import sprout.mvc.http.ResponseCode;
import sprout.mvc.http.ResponseEntity;
import sprout.security.authorization.AuthorizationRule;
import sprout.security.authorization.exception.AccessDeniedException;
import sprout.security.context.SecurityContextHolder;
import sprout.security.core.Authentication;
import sprout.security.core.GrantedAuthority;
import sprout.security.web.util.matcher.RequestMatcher;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class AuthorizationFilter implements Filter, InfrastructureBean {

    private final List<AuthorizationRule> authorizationRules;

    public AuthorizationFilter(List<AuthorizationRule> authorizationRules) {
        this.authorizationRules = authorizationRules;
    }

    @Override
    public void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) throws IOException {
        Optional<AuthorizationRule> matchedRule = authorizationRules.stream()
                .filter(rule -> rule.getRequestMatcher().matches(request))
                .findFirst(); // 가장 먼저 매칭되는 규칙 사용

        if (matchedRule.isPresent()) {
            AuthorizationRule rule = matchedRule.get();

            if (rule.isPermitAll()) {
                chain.doFilter(request, response);
                return;
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                ResponseEntity<?> responseEntity = new ResponseEntity<>("Authentication required.", null, ResponseCode.UNAUTHORIZED);
                response.setResponseEntity(responseEntity);
                return;
            }

            if (rule.isAuthenticated()) {
                chain.doFilter(request, response);
                return;
            }

            if (!rule.getRequiredAuthorities().isEmpty()) {
                Collection<? extends GrantedAuthority> userAuthorities = Optional.ofNullable(authentication.getAuthorities())
                        .orElse(Collections.emptyList());

                Set<String> userAuthorityStrings = userAuthorities.stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet());

                boolean hasAnyRequiredAuthority = rule.getRequiredAuthorities().stream()
                        .anyMatch(userAuthorityStrings::contains);

                if (hasAnyRequiredAuthority) {
                    chain.doFilter(request, response);
                    return;
                } else {
                    throw new AccessDeniedException("Access Denied: User '" + authentication.getPrincipal() +
                            "' does not have any of the required authorities: " + rule.getRequiredAuthorities());
                }
            }
            // 규칙이 있지만 어떤 조건에도 해당하지 않는 경우 (방어적)
            throw new AccessDeniedException("Access Denied: No matching authorization criteria for authenticated user.");

        } else {
            // 여기서는 매칭되는 규칙이 없으면 필터 체인 통과 (다음 필터 또는 컨트롤러로)
            chain.doFilter(request, response);
        }
    }
}
