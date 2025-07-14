package sprout.security.autoconfiguration.annotation;

import sprout.beans.annotation.Configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Configuration
public @interface EnableSproutSecurity {
    boolean defaultSecurityDisabled() default false;
}
