package sprout.aop.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AfterThrowing {
    Class<? extends Annotation>[] annotation();
    String pointcut() default "";
}
