package sprout.aop.advice.builder;

import sprout.aop.JoinPoint;
import sprout.aop.ProceedingJoinPoint;
import sprout.aop.advice.interceptor.SimpleAroundInterceptor;
import sprout.aop.advice.Advice;
import sprout.aop.advice.AdviceBuilder;
import sprout.aop.advisor.Advisor;
import sprout.aop.advisor.DefaultAdvisor;
import sprout.aop.advisor.Pointcut;
import sprout.aop.advisor.PointcutFactory;
import sprout.aop.annotation.Around;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Supplier;

public class AroundAdviceBuilder implements AdviceBuilder {
    @Override
    public Advisor build(Class<?> aspectCls, Method method,
                         Supplier<Object> sup, PointcutFactory pf) {

        Around around = method.getAnnotation(Around.class);

        if (method.getParameterCount() > 1 ||
                (method.getParameterCount() == 1 &&
                        !ProceedingJoinPoint.class.isAssignableFrom(method.getParameterTypes()[0]))) {
            throw new IllegalStateException("@Before method must have 0 or 1 JoinPoint param");
        }


        Pointcut pc = pf.createPointcut(around.annotation(), around.pointcut());

        // static 여부에 따라 Supplier 결정
        Supplier<Object> safe = Modifier.isStatic(method.getModifiers()) ? () -> null : sup;

        Advice advice = new SimpleAroundInterceptor(safe, method);
        return new DefaultAdvisor(pc, advice, 0);
    }
}
