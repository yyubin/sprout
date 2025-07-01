package sprout.aop.advisor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class AnnotationPointcut implements Pointcut {
    private final Class<? extends Annotation> annotationType;

    public AnnotationPointcut(Class<? extends Annotation> annotationType) {
        this.annotationType = annotationType;
    }

    @Override
    public boolean matches(Class<?> targetClass, Method method) {
        // 클래스 레벨 어노테이션은 현재 고려하지 않고, 메서드에만 적용
        return method.isAnnotationPresent(annotationType);
    }
}
