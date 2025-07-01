package sprout.aop.advisor;

import java.lang.reflect.Method;

public interface Pointcut {
    boolean matches(Class<?> targetClass, Method method);
}
