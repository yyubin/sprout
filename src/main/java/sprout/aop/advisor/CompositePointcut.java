package sprout.aop.advisor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class CompositePointcut implements Pointcut {
    private final List<Pointcut> pointcuts;

    public CompositePointcut(Pointcut... pointcuts) {
        this.pointcuts = Arrays.asList(pointcuts);
    }

    public CompositePointcut(List<Pointcut> pointcuts) {
        this.pointcuts = pointcuts;
    }

    @Override
    public boolean matches(Class<?> targetClass, Method method) {
        for (Pointcut pointcut : pointcuts) {
            if (pointcut.matches(targetClass, method)) {
                return true;
            }
        }
        return false;
    }
}