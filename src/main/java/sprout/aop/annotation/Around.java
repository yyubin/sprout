package sprout.aop.annotation;

import test.aop.Auth;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Around {
    Class<? extends Annotation>[] annotation();
    String pointcut() default "";
}
