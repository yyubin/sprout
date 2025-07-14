package sprout.security.context;

import sprout.security.core.SecurityContext;

import java.util.function.Supplier;

final class ThreadLocalSecurityContextHolderStrategy implements SecurityContextHolderStrategy{

    private static final ThreadLocal<Supplier<SecurityContext>> contextHolder = new ThreadLocal<>();

    @Override
    public void clearContext() {
        contextHolder.remove();
    }

    @Override
    public SecurityContext getContext() {
        return getDeferredContext().get();
    }

    @Override
    public Supplier<SecurityContext> getDeferredContext() {
        Supplier<SecurityContext> result = contextHolder.get();
        if (result == null) {
            SecurityContext context = createEmptyContext();
            result = () -> context;
            contextHolder.set(result);
        }
        return result;
    }

    @Override
    public void setContext(SecurityContext context) {
        contextHolder.set(() -> context);
    }

    @Override
    public void setDeferredContext(Supplier<SecurityContext> deferredContext) {
        contextHolder.set(deferredContext);
    }

    @Override
    public SecurityContext createEmptyContext() {
        return new SecurityContextImpl(null);
    }
}
