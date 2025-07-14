package sprout.security.authorization.aop;

import sprout.aop.JoinPoint;
import sprout.aop.annotation.Aspect;
import sprout.aop.annotation.Before;
import sprout.security.authorization.annotation.PreAuthorize;
import sprout.security.context.SecurityContextHolder;
import sprout.security.core.Authentication;
import sprout.security.core.GrantedAuthority;

import java.lang.reflect.Method;
import java.nio.file.AccessDeniedException;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

@Aspect
public class AuthorizationAspect {

    @Before(annotation = PreAuthorize.class)
    public void preAuthorize(JoinPoint joinPoint) throws AccessDeniedException {
        Method method = joinPoint.getMethod();
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);

        if (annotation == null) {
            return;
        }

        Collection<? extends GrantedAuthority> authorities = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getAuthorities)
                .orElse(Collections.emptyList());

        String requiredAuthority = annotation.value();

        if (!requiredAuthority.isBlank()) { // value가 비어있지 않은 경우에만 검사
            boolean hasRequiredAuthority = authorities.stream()
                    .anyMatch(a -> a.getAuthority().equals(requiredAuthority));

            if (!hasRequiredAuthority) {
                throw new AccessDeniedException("Access Denied: User '" +
                        Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                                .map(Authentication::getPrincipal)
                                .map(Object::toString)
                                .orElse("anonymous") +
                        "' does not have required authority '" + requiredAuthority + "' for method " + method.getName());
            }
        } else {
            System.err.println("[WARN] @PreAuthorize annotation on method " + method.getName() + " has no value defined. Access will be granted by default.");
        }
    }
}
