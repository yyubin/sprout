package sprout.aop.advice;

import sprout.aop.advisor.Advisor;
import sprout.aop.advisor.PointcutFactory;

import java.lang.reflect.Method;
import java.util.function.Supplier;

@FunctionalInterface
public interface AdviceBuilder {
    Advisor build(Class<?> aspectCls,
                  Method method,
                  Supplier<Object> aspectSup,
                  PointcutFactory pcf);
}