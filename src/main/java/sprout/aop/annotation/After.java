package sprout.aop.annotation;
import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface After {
    Class<? extends Annotation>[] annotation();
    String pointcut() default "";
}
