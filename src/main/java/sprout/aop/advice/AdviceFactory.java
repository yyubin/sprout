package sprout.aop.advice;

import sprout.aop.AspectMethodInterceptor;
import sprout.aop.advisor.Advisor;
import sprout.aop.advisor.DefaultAdvisor;
import sprout.aop.advisor.Pointcut;
import sprout.aop.advisor.PointcutFactory;
import sprout.aop.annotation.Around;
import sprout.beans.annotation.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.function.Supplier;

@Component
public class AdviceFactory {
    private final PointcutFactory pointcutFactory;

    public AdviceFactory(PointcutFactory pointcutFactory) {
        this.pointcutFactory = pointcutFactory;
    }

    public Optional<Advisor> createAdvisor(Class<?> aspectClass, Method method, Supplier<Object> aspectSupplier) {
        if (method.isAnnotationPresent(Around.class)) {
            Around around = method.getAnnotation(Around.class);

            Pointcut pointcut =
                    pointcutFactory.createPointcut(
                            around.annotation(),   // Class<? extends Annotation>[]
                            around.pointcut()      // regex
                    );

            Supplier<Object> safeSupplier =
                    (Modifier.isStatic(method.getModifiers()) ? () -> null : aspectSupplier);

            Advice advice = new AspectMethodInterceptor(safeSupplier, method);
            return Optional.of(new DefaultAdvisor(pointcut, advice, 0));
        }

        /* ---------- TODO: @After, @Before, ... ---------- */

        return Optional.empty();
    }
}
