package sprout.security.context;

import sprout.beans.annotation.Component;
import sprout.config.AppConfig;
import sprout.context.BeanFactory;
import sprout.context.ContextInitializer;

@Component
public class SecurityContextInitializer implements ContextInitializer {

    @Override
    public void initializeAfterRefresh(BeanFactory context) {
        SecurityContextHolder.initialize(context.getBean(AppConfig.class));
    }
}
