package sprout.aop.advice.builder;

import sprout.aop.JoinPoint;
import sprout.aop.advice.Advice;
import sprout.aop.advice.AdviceBuilder;
import sprout.aop.advice.interceptor.SimpleBeforeInterceptor;
import sprout.aop.advisor.Advisor;
import sprout.aop.advisor.DefaultAdvisor;
import sprout.aop.advisor.Pointcut;
import sprout.aop.advisor.PointcutFactory;
import sprout.aop.annotation.Before;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Supplier;

public class BeforeAdviceBuilder implements AdviceBuilder {
    @Override
    public Advisor build(Class<?> aspectCls,
                         Method method,
                         Supplier<Object> aspectSup,
                         PointcutFactory pf) {

        Before before = method.getAnnotation(Before.class);

        if (method.getParameterCount() > 1 ||
                (method.getParameterCount() == 1 &&
                        !JoinPoint.class.isAssignableFrom(method.getParameterTypes()[0]))) {
            throw new IllegalStateException("@Before method must have 0 or 1 JoinPoint param");
        }

        Pointcut pc = pf.createPointcut(before.annotation(),
                before.pointcut());

        Supplier<Object> safe = Modifier.isStatic(method.getModifiers())
                ? () -> null
                : aspectSup;

        Advice advice = new SimpleBeforeInterceptor(safe, method);
        return new DefaultAdvisor(pc, advice, 0);
    }
}
