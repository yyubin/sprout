package sprout.aop.config;

import sprout.aop.AspectPostProcessor;
import sprout.aop.advice.AdviceFactory;
import sprout.aop.advisor.AdvisorRegistry;
import sprout.aop.advisor.DefaultPointcutFactory;
import sprout.aop.advisor.PointcutFactory;
import sprout.beans.annotation.Bean;
import sprout.beans.annotation.Configuration;
import sprout.context.Container;

// @Configuration(proxyBeanMethods = true)
public class AopAutoConfiguration {
    @Bean
    public AdvisorRegistry advisorRegistry() {
        return new AdvisorRegistry();
    }

    @Bean
    public PointcutFactory pointcutFactory() {
        return new DefaultPointcutFactory();
    }

    @Bean
    public AdviceFactory adviceFactory(PointcutFactory pointcutFactory) {
        return new AdviceFactory(pointcutFactory);
    }

    @Bean
    public AspectPostProcessor aspectPostProcessor(
            AdvisorRegistry advisorRegistry,
            Container container,
            AdviceFactory adviceFactory) {
        return new AspectPostProcessor(advisorRegistry, container, adviceFactory);
    }
}
