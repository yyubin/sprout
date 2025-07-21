package sprout.context;

import java.util.List;

public interface PostInfrastructureInitializer {
    void afterInfrastructureSetup(BeanFactory beanFactory, List<String> basePackages);
}
