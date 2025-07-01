package sprout.aop.advice;

import sprout.aop.AspectMethodInterceptor;
import sprout.aop.advisor.Advisor;
import sprout.aop.advisor.DefaultAdvisor;
import sprout.aop.advisor.Pointcut;
import sprout.aop.advisor.PointcutFactory;
import sprout.aop.annotation.Around;
import sprout.beans.annotation.Component;

import java.lang.reflect.Method;
import java.util.Optional;

@Component
public class AdviceFactory {
    private final PointcutFactory pointcutFactory;

    public AdviceFactory(PointcutFactory pointcutFactory) {
        this.pointcutFactory = pointcutFactory;
    }

    public Optional<Advisor> createAdvisor(Object aspectInstance, Method method) {
        if (method.isAnnotationPresent(Around.class)) {
            Around around = method.getAnnotation(Around.class);
            Pointcut pointcut = pointcutFactory.createPointcut(around.annotation(), around.pointcut());

            Advice aroundAdvice = new AspectMethodInterceptor(aspectInstance, method);
            return Optional.of(new DefaultAdvisor(pointcut, aroundAdvice, 1));
        }
        // TODO: @After 어노테이션도 유사하게 처리
        // if (method.isAnnotationPresent(After.class)) {
        //     After after = method.getAnnotation(After.class);
        //     Pointcut pointcut = pointcutFactory.createPointcut(after.annotation(), after.pointcut());
        //     int order = after.order();
        //     Advice afterAdvice = new AfterAdvice(aspectInstance, method); // AfterAdvice도 sprout.aop.Advice를 구현해야 함
        //     return Optional.of(new Advisor(pointcut, afterAdvice, order));
        // }

        return Optional.empty(); // 해당 어노테이션이 없으면 빈 Optional 반환
    }
}
