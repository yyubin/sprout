package sprout.aop.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Around {
    Class<? extends Annotation>[] annotation() default {};
    String pointcut() default "";
}
