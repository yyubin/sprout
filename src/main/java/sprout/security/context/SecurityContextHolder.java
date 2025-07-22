package sprout.security.context;

import sprout.config.AppConfig;
import sprout.security.core.SecurityContext;

import java.util.function.Supplier;

public final class SecurityContextHolder {

    private static SecurityContextHolderStrategy strategy;
    private static final String DEFAULT_STRATEGY = "bio";


    public static synchronized void initialize(AppConfig appConfig) {
        if (strategy != null) return;
        String strategyName = appConfig.getStringProperty("sprout.security.strategy", DEFAULT_STRATEGY);
        if ("nio".equals(strategyName)) {
            strategy = new ChannelAwareSecurityContextHolderStrategy();
            System.out.println("SecurityContextHolder initialized with ChannelAware strategy.");
        } else {
            strategy = new ThreadLocalSecurityContextHolderStrategy();
            System.out.println("SecurityContextHolder initialized with ThreadLocal strategy.");
        }

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
