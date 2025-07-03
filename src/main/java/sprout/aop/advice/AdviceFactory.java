package sprout.aop.advice;

import sprout.aop.advice.builder.AfterAdviceBuilder;
import sprout.aop.advice.builder.AroundAdviceBuilder;
import sprout.aop.advice.builder.BeforeAdviceBuilder;
import sprout.aop.advisor.Advisor;
import sprout.aop.advisor.PointcutFactory;
import sprout.beans.InfrastructureBean;
import sprout.beans.annotation.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Component
public class AdviceFactory implements InfrastructureBean {
    private final Map<AdviceType, AdviceBuilder> builders;
    private final PointcutFactory pointcutFactory;

    public AdviceFactory(PointcutFactory pointcutFactory) {
        this.pointcutFactory = pointcutFactory;
        this.builders = Map.of(
                AdviceType.AROUND, new AroundAdviceBuilder(),
                AdviceType.BEFORE, new BeforeAdviceBuilder(),
                AdviceType.AFTER,  new AfterAdviceBuilder()
        );
    }

    public Optional<Advisor> createAdvisor(Class<?> aspectCls,
                                           Method m,
                                           Supplier<Object> sup) {

        return AdviceType.from(m)
                .map(type -> builders.get(type)
                        .build(aspectCls, m, sup, pointcutFactory));
    }
}
