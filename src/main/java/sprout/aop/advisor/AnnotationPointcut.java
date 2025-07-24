package sprout.aop.advisor;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

public class AnnotationPointcut implements Pointcut {
    private final Class<? extends Annotation> annotationType;

    public AnnotationPointcut(Class<? extends Annotation> annotationType) {
        this.annotationType = annotationType;
    }

    @Override
    public boolean matches(Class<?> targetClass, Method method) {
        // 1) method에 직접
        if (has(method)) return true;

        // 2) declaring class, 실제 targetClass, 인터페이스까지
        if (has(method.getDeclaringClass()) || has(targetClass)) return true;

        // 3) (옵션) 파라미터 애노테이션
        // for (Annotation[] anns : method.getParameterAnnotations())
        //   for (Annotation a : anns)
        //     if (annotationType == a.annotationType()) return true;

        return false;
    }

    private boolean has(AnnotatedElement el) {
        return el.isAnnotationPresent(annotationType);
    }
}
