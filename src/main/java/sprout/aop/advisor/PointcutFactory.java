package sprout.aop.advisor;

import java.lang.annotation.Annotation;

public interface PointcutFactory {
    Pointcut createPointcut(Class<? extends Annotation>[] annotationTypes, String regexExpression);
}
