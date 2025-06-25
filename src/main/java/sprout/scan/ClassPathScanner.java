package sprout.scan;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import sprout.aop.annotation.BeforeAuthCheck;
import sprout.beans.BeanCreationMethod;
import sprout.beans.BeanDefinition;
import sprout.beans.ConstructorBeanDefinition;
import sprout.beans.MethodBeanDefinition;
import sprout.beans.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ClassPathScanner {

    public Collection<BeanDefinition> scan(ConfigurationBuilder configBuilder) {
        configBuilder.addScanners(Scanners.TypesAnnotated, Scanners.SubTypes);
        Reflections r = new Reflections(configBuilder);

        // @Component, @Service 등 어노테이션 기반의 빈 타입 탐색
        Set<Class<?>> componentCandidates = new HashSet<>();
        componentCandidates.addAll(r.getTypesAnnotatedWith(Component.class));
        componentCandidates.addAll(r.getTypesAnnotatedWith(Controller.class));
        componentCandidates.addAll(r.getTypesAnnotatedWith(Service.class));
        componentCandidates.addAll(r.getTypesAnnotatedWith(Repository.class));
        componentCandidates.addAll(r.getTypesAnnotatedWith(Configuration.class));

        Set<Class<?>> concreteComponentTypes = componentCandidates.stream()
                .filter(clazz -> !clazz.isInterface() && !clazz.isAnnotation() && !Modifier.isAbstract(clazz.getModifiers()))
                .collect(Collectors.toSet());

        // @Bean 메서드 기반의 빈 타입 탐색
        Set<Class<?>> beanMethodReturnTypes = new HashSet<>();
        Set<Class<?>> configClasses = r.getTypesAnnotatedWith(Configuration.class);
        for (Class<?> configClass : configClasses) {
            for (Method method : configClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Bean.class)) {
                    beanMethodReturnTypes.add(method.getReturnType());
                }
            }
        }

        // DI 컨테이너가 알게 될 모든 빈의 타입을 통합
        Set<Class<?>> allKnownBeanTypes = new HashSet<>();
        allKnownBeanTypes.addAll(concreteComponentTypes);
        allKnownBeanTypes.addAll(beanMethodReturnTypes);

        List<BeanDefinition> definitions = new ArrayList<>();

        // @Component 등 일반 빈 처리
        for (Class<?> clazz : concreteComponentTypes) {
            try {
                Constructor<?> ctor = resolveUsableConstructor(clazz, allKnownBeanTypes);
                ConstructorBeanDefinition def = new ConstructorBeanDefinition(
                        generateBeanName(clazz),
                        clazz,
                        ctor,
                        ctor.getParameterTypes()
                );
                if (clazz.isAnnotationPresent(Configuration.class)) {
                    def.setConfigurationClassProxyNeeded(clazz.getAnnotation(Configuration.class).proxyBeanMethods());
                }
                definitions.add(def);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("No usable constructor for class " + clazz.getName(), e);
            }
        }

        // @Bean 메서드 처리
        for (Class<?> configClass : configClasses) {
            for (Method method : configClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Bean.class)) {
                    if (Modifier.isStatic(method.getModifiers()) || method.getReturnType() == void.class) continue;

                    MethodBeanDefinition def = new MethodBeanDefinition(
                            generateBeanName(method),
                            method.getReturnType(),
                            method,
                            generateBeanName(configClass),
                            method.getParameterTypes()
                    );
                    definitions.add(def);
                }
            }
        }

        // 디버깅 출력
        definitions.forEach(d -> System.out.println("→ BeanDefinition: Name=" + d.getName() + ", Type=" + d.getType().getSimpleName()));

        return definitions;
    }

    // 가장 많은 인자를 가지는 해결 가능한 생성자를 선호 (Spring의 기본 전략)
    private Constructor<?> resolveUsableConstructor(Class<?> clazz, Set<Class<?>> allKnownBeanTypes) throws NoSuchMethodException {
        return Arrays.stream(clazz.getDeclaredConstructors())
                .filter(constructor -> Arrays.stream(constructor.getParameterTypes())
                        .allMatch(param -> isResolvable(param, allKnownBeanTypes))) // 수정된 allKnownBeanTypes 사용
                .max(Comparator.comparingInt(Constructor::getParameterCount))
                .orElseThrow(() -> new NoSuchMethodException("No usable constructor for " + clazz.getName() + " with known types."));
    }

    private boolean isResolvable(Class<?> paramType, Set<Class<?>> allKnownBeanTypes) {
        if (List.class.isAssignableFrom(paramType)) {
            return true; // List 주입은 항상 가능하다고 가정
        }
        // allKnownBeanTypes에 파라미터 타입과 일치하거나, 해당 타입을 구현/상속하는 타입이 있는지 확인
        return allKnownBeanTypes.stream().anyMatch(knownType -> paramType.isAssignableFrom(knownType));
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