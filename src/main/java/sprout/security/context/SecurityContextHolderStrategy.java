package sprout.security.context;

import sprout.security.core.SecurityContext;

import java.util.function.Supplier;

public interface SecurityContextHolderStrategy {
    void clearContext();
    SecurityContext getContext();
    default Supplier<SecurityContext> getDeferredContext() {
        return this::getContext;
    }
    void setContext(SecurityContext context);
    default void setDeferredContext(Supplier<SecurityContext> deferredContext) {
        setContext(deferredContext.get());
    }
    SecurityContext createEmptyContext();
}
