package sprout.scan;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import sprout.aop.annotation.BeforeAuthCheck;
import sprout.beans.BeanCreationMethod;
import sprout.beans.BeanDefinition;
import sprout.beans.ConstructorBeanDefinition; // ConstructorBeanDefinition 구현체 추가
import sprout.beans.MethodBeanDefinition;     // MethodBeanDefinition 구현체 추가
import sprout.beans.annotation.*; // 모든 어노테이션 임포트

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method; // Method 임포트 추가
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ClassPathScanner {

    public Collection<BeanDefinition> scan(ConfigurationBuilder configBuilder) {
        configBuilder.addScanners(new TypeAnnotationsScanner(), new SubTypesScanner());
        Reflections r = new Reflections(configBuilder);

        Set<Class<?>> componentCandidates = new HashSet<>();
        componentCandidates.addAll(r.getTypesAnnotatedWith(Component.class));
        componentCandidates.addAll(r.getTypesAnnotatedWith(Controller.class));
        componentCandidates.addAll(r.getTypesAnnotatedWith(Service.class));
        componentCandidates.addAll(r.getTypesAnnotatedWith(Repository.class));
        componentCandidates.addAll(r.getTypesAnnotatedWith(Configuration.class));

        // 인터페이스, 어노테이션, 추상 클래스를 제외한 실제 인스턴스화 가능한 클래스들
        Set<Class<?>> concreteBeanTypes = componentCandidates.stream()
                .filter(clazz -> !clazz.isInterface() && !clazz.isAnnotation() && !Modifier.isAbstract(clazz.getModifiers()))
                .collect(Collectors.toSet());

        // 빈 정의를 저장할 리스트
        List<BeanDefinition> definitions = new ArrayList<>();

        // @Component/@Service 등 일반 빈과 @Configuration 클래스 자체를 처리
        for (Class<?> clazz : concreteBeanTypes) {
            try {
                Constructor<?> ctor = resolveUsableConstructor(clazz, concreteBeanTypes);

                ConstructorBeanDefinition def = new ConstructorBeanDefinition(
                        generateBeanName(clazz), // 빈 이름 생성
                        clazz,
                        ctor,
                        ctor.getParameterTypes()
                );

                // AOP 프록시 필요 여부 설정 (BeforeAuthCheck 예시)
                // def.setProxyTarget(clazz.isAnnotationPresent(BeforeAuthCheck.class));

                // @Configuration 클래스인 경우, isConfigurationClassProxyNeeded 설정
                if (clazz.isAnnotationPresent(Configuration.class)) {
                    Configuration configAnno = clazz.getAnnotation(Configuration.class);
                    def.setConfigurationClassProxyNeeded(configAnno.proxyBeanMethods());
                }

                definitions.add(def);

            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("No usable constructor for class " + clazz.getName(), e);
            }
        }

        // @Configuration 클래스 내의 @Bean 메서드를 스캔하여 BeanDefinition 추가
        Set<Class<?>> configClasses = r.getTypesAnnotatedWith(Configuration.class);
        for (Class<?> configClass : configClasses) {
            for (Method method : configClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Bean.class)) {
                    // @Bean 메서드는 static이 아니어야 하고, @Configuration 클래스의 메서드여야 함
                    if (Modifier.isStatic(method.getModifiers())) {
                        System.err.println("Warning: @Bean method '" + method.getName() + "' in '" + configClass.getName() + "' is static. This might not behave as expected with proxyBeanMethods=true.");
                        // 스태틱 @Bean 메서드도 처리할 수 있지만 여기서는 일단 무시
                        // Spring은 static @Bean 메서드를 지원하지만, proxyBeanMethods=true 일 때 약간 다르게 동작함..
                        continue;
                    }

                    // @Bean 메서드의 반환 타입이 빈의 타입이 됩니다.
                    Class<?> beanType = method.getReturnType();
                    // void 타입의 @Bean 메서드는 유효하지 않음
                    if (beanType == void.class) {
                        throw new IllegalStateException("@Bean method '" + method.getName() + "' in '" + configClass.getName() + "' returns void.");
                    }

                    // MethodBeanDefinition 생성
                    MethodBeanDefinition def = new MethodBeanDefinition(
                            generateBeanName(method), // 빈 이름 생성 (메서드 이름 기반)
                            beanType,
                            method,
                            generateBeanName(configClass), // 팩토리 빈의 이름 (Configuration 클래스 빈의 이름)
                            method.getParameterTypes()
                    );
                    // @Bean 메서드로 생성되는 빈도 AOP 대상이 될 수 있음
                    // def.setProxyTarget(method.isAnnotationPresent(BeforeAuthCheck.class));

                    definitions.add(def);
                }
            }
        }

        // 디버깅을 위한 출력
        definitions.forEach(d -> System.out.println("→ BeanDefinition: Name=" + d.getName() + ", Type=" + d.getType().getSimpleName() + ", CreationMethod=" + d.getCreationMethod() + ", ConfigProxy=" + d.isConfigurationClassProxyNeeded() + ", AopProxy=" + d.isProxyTarget()));

        return definitions;
    }

    // 가장 많은 인자를 가지는 해결 가능한 생성자를 선호 (Spring의 기본 전략)
    private Constructor<?> resolveUsableConstructor(Class<?> clazz, Set<Class<?>> knownBeanTypes) throws NoSuchMethodException {
        return Arrays.stream(clazz.getDeclaredConstructors())
                // 매개변수들이 모두 알려진 빈 타입이거나 List 타입인 생성자만 필터링
                .filter(constructor -> Arrays.stream(constructor.getParameterTypes())
                        .allMatch(param -> isResolvable(param, knownBeanTypes)))
                // 가장 많은 매개변수를 가진 생성자를 선택 (greedy strategy)
                .max(Comparator.comparingInt(Constructor::getParameterCount))
                .orElseThrow(() -> new NoSuchMethodException("No usable constructor for " + clazz.getName()));
    }

    private boolean isResolvable(Class<?> paramType, Set<Class<?>> knownBeanTypes) {
        // 이미 스캔된 구체적인 빈 타입인 경우
        if (knownBeanTypes.contains(paramType)) {
            return true;
        }
        // List 타입인 경우 (나중에 List<T> 형태로 주입될 것임)
        if (List.class.isAssignableFrom(paramType)) {
            return true;
        }
        // 인터페이스인 경우, 해당 인터페이스를 구현하는 알려진 구체적인 빈 타입이 하나라도 있다면 해결 가능
        if (paramType.isInterface()) {
            return knownBeanTypes.stream().anyMatch(c -> paramType.isAssignableFrom(c));
        }

        return false;
    }

    private String generateBeanName(Class<?> clazz) {
        String className = clazz.getSimpleName();
        if (className.length() > 0) {
            return Character.toLowerCase(className.charAt(0)) + className.substring(1);
        }
        return className;
    }


    private String generateBeanName(Method method) {
        Bean beanAnno = method.getAnnotation(Bean.class);
        if (beanAnno != null && !beanAnno.value().isEmpty()) {
            return beanAnno.value();
        }
        return method.getName();
    }
}