package sprout.security.autoconfiguration;

import sprout.beans.BeanDefinition;
import sprout.beans.ConstructorBeanDefinition;
import sprout.beans.InfrastructureBean;
import sprout.beans.annotation.Component;
import sprout.beans.processor.BeanDefinitionRegistrar;
import sprout.config.AppConfig;
import sprout.security.authentication.*;
import sprout.security.authentication.exception.UsernameNotFoundException;
import sprout.security.authentication.password.BCryptPasswordEncoder;
import sprout.security.authentication.password.PasswordEncoder;
import sprout.security.autoconfiguration.annotation.EnableSproutSecurity;
import sprout.security.core.UserDetailsService;
import sprout.security.filter.AuthenticationFilter;
import sprout.security.web.util.matcher.AntPathRequestMatcher;
import sprout.security.web.util.matcher.RequestMatcher;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class SecurityAutoConfigurationRegistrar implements BeanDefinitionRegistrar {
    @Override
    public Collection<BeanDefinition> registerAdditionalBeanDefinitions(Collection<BeanDefinition> existingDefs) throws NoSuchMethodException {
        List<BeanDefinition> additionalDefs = new ArrayList<>();
        boolean hasEnabledSproutSecurity = false;
        EnableSproutSecurity enableSproutSecurity = null;
        for (BeanDefinition def : existingDefs) {
            if (def.getType().isAnnotationPresent(EnableSproutSecurity.class)) {
                hasEnabledSproutSecurity = true;
                enableSproutSecurity = def.getType().getAnnotation(EnableSproutSecurity.class);
            }
        }
        if (!hasEnabledSproutSecurity || (enableSproutSecurity != null && enableSproutSecurity.defaultSecurityDisabled())) {
            System.out.println("No @EnableSproutSecurity found. Registering default security beans.");

            if (existingDefs.stream().noneMatch(def -> PasswordEncoder.class.isAssignableFrom(def.getType()))) {
                Constructor<?> constructor = BCryptPasswordEncoder.class.getConstructor();
                Class<?>[] constructorArgumentsTypes = constructor.getParameterTypes();
                additionalDefs.add(new ConstructorBeanDefinition("passwordEncoder", BCryptPasswordEncoder.class, constructor, constructorArgumentsTypes));
            }

            if (existingDefs.stream().noneMatch(def -> UserDetailsService.class.isAssignableFrom(def.getType()))) {
                Constructor<?> constructor = DefaultUserDetailsService.class.getConstructor(AppConfig.class, PasswordEncoder.class);
                Class<?>[] constructorArgumentsTypes = constructor.getParameterTypes();
                additionalDefs.add(new ConstructorBeanDefinition("userDetailsService", DefaultUserDetailsService.class, constructor, constructorArgumentsTypes));
            }

            if (existingDefs.stream().noneMatch(def -> DaoAuthenticationProvider.class.isAssignableFrom(def.getType()))) {
                Constructor<?> constructor = DaoAuthenticationProvider.class.getConstructor(UserDetailsService.class, PasswordEncoder.class);
                Class<?>[] constructorArgumentsTypes = constructor.getParameterTypes();
                additionalDefs.add(new ConstructorBeanDefinition("daoAuthenticationProvider", DaoAuthenticationProvider.class, constructor, constructorArgumentsTypes));
            }

            if (existingDefs.stream().noneMatch(def -> RequestMatcher.class.isAssignableFrom(def.getType()))) {
                // '/' 경로에 대한 RequestMatcher 기본값
                Constructor<?> constructor = AntPathRequestMatcher.class.getConstructor(String.class);
                Class<?>[] constructorArgumentsTypes = constructor.getParameterTypes();
                Object[] constructorArguments = new Object[]{"/login"}; // <-- 실제 인자 값
                additionalDefs.add(new ConstructorBeanDefinition("defaultRequestMatcher", AntPathRequestMatcher.class, constructor, constructorArgumentsTypes, constructorArguments));
            }

            if (existingDefs.stream().noneMatch(def -> AuthenticationManager.class.isAssignableFrom(def.getType()))) {
                // List<AuthenticationProvider>는 컨테이너가 자동으로 찾아 주입할 것을 기대
                // 이 생성자는 List<AuthenticationProvider>만 필요로 함
                Constructor<?> constructor = ProviderManager.class.getConstructor(List.class);
                Class<?>[] constructorArgumentsTypes = constructor.getParameterTypes();
                // 여기에 실제 인자 값을 넘기지 않으면, 컨테이너가 의존성 주입 로직을 통해 List<AuthenticationProvider>를 채워줄 것임
                additionalDefs.add(new ConstructorBeanDefinition("authenticationManager", ProviderManager.class, constructor, constructorArgumentsTypes));
            }

            if (existingDefs.stream().noneMatch(def -> AuthenticationFilter.class.isAssignableFrom(def.getType()))) {
                Constructor<?> constructor = AuthenticationFilter.class.getConstructor(List.class, AuthenticationManager.class);
                Class<?>[] constructorArgumentsTypes = constructor.getParameterTypes();
                additionalDefs.add(new ConstructorBeanDefinition("authenticationFilter", AuthenticationFilter.class, constructor, constructorArgumentsTypes));
            }
        }
        return additionalDefs;
    }
}

