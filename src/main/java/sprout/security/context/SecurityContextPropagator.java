package sprout.security.context;

import sprout.beans.annotation.Component;
import sprout.beans.annotation.Order;
import sprout.context.ContextPropagator;
import sprout.security.core.SecurityContext;

@Component
@Order(20)
public class SecurityContextPropagator implements ContextPropagator<SecurityContext> {
    @Override
    public SecurityContext capture() {
        return SecurityContextHolder.getContext();
    }

    @Override
    public void restore(SecurityContext ctx) {
        SecurityContextHolder.setContext(ctx);
    }
    @Override
    public void clear() {
        SecurityContextHolder.clearContext();
    }
}
