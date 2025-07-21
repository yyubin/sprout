package sprout.security.context;

import sprout.beans.annotation.Component;
import sprout.beans.annotation.Order;
import sprout.context.ContextPropagator;
import sprout.security.core.SecurityContext;

@Component
@Order(20)
public class SecurityContextPropagator implements ContextPropagator {
    private SecurityContext context;

    @Override
    public void capture() {
        context = SecurityContextHolder.getContext();
    }

    @Override
    public void restore() {
        SecurityContextHolder.setContext(context);
    }

    @Override
    public void clear() {
        SecurityContextHolder.clearContext();
    }
}
