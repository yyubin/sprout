package sprout.security.context;

import sprout.security.UserPrincipal;
import sprout.security.core.SecurityContext;

import java.lang.reflect.Constructor;
import java.util.function.Supplier;

public final class SecurityContextHolder {

    private static SecurityContextHolderStrategy strategy;

    static {
        initialize();
    }

    private static void initialize() {
        initializeStrategy();
    }

    private static void initializeStrategy() {
        strategy = new ThreadLocalSecurityContextHolderStrategy();
    }

    public static void clearContext() {
        strategy.clearContext();
    }

    public static SecurityContext getContext() {
        return strategy.getContext();
    }

    public static Supplier<SecurityContext> getDeferredContext() {
        return strategy.getDeferredContext();
    }

    public static void setContext(SecurityContext context) {
        strategy.setContext(context);
    }

    public static void setDeferredContext(Supplier<SecurityContext> deferredContext) {
        strategy.setDeferredContext(deferredContext);
    }

    public static SecurityContext createEmptyContext() {
        return strategy.createEmptyContext();
    }

    public static SecurityContextHolderStrategy getContextHolderStrategy() {
        return strategy;
    }
}
