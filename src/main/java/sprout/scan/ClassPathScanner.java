package sprout.scan;

import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import sprout.aop.annotation.BeforeAuthCheck;
import sprout.beans.BeanDefinition;
import org.reflections.Reflections;
import sprout.beans.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ClassPathScanner {
    public Collection<BeanDefinition> scan(ConfigurationBuilder configBuilder) {
        Reflections r = new Reflections(configBuilder);

        Set<Class<?>> componentCandidates = new HashSet<>();
        componentCandidates.addAll(r.getTypesAnnotatedWith(Component.class));
        componentCandidates.addAll(r.getTypesAnnotatedWith(Controller.class));
        componentCandidates.addAll(r.getTypesAnnotatedWith(Service.class));
        componentCandidates.addAll(r.getTypesAnnotatedWith(Repository.class));

        Set<Class<?>> concreteBeanTypes = componentCandidates.stream()
                .filter(clazz -> !clazz.isInterface() && !clazz.isAnnotation() && !Modifier.isAbstract(clazz.getModifiers()))
                .collect(Collectors.toSet());
        Set<Class<?>> knownTypes = new HashSet<>(concreteBeanTypes);

        List<BeanDefinition> definitions = new ArrayList<>();

        for (Class<?> clazz : knownTypes) {
            try {
                Constructor<?> ctor = resolveUsableConstructor(clazz, knownTypes);
                boolean proxy = clazz.isAnnotationPresent(BeforeAuthCheck.class);
                definitions.add(new BeanDefinition(clazz, ctor, ctor.getParameterTypes(), proxy));
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("No usable constructor for class " + clazz.getName(), e);
            }
        }
        definitions.forEach(d -> System.out.println("→ "+ d.type()));

        return definitions;
    }

    private Constructor<?> resolveUsableConstructor(Class<?> clazz, Set<Class<?>> knownTypes) throws NoSuchMethodException {
        return Arrays.stream(clazz.getDeclaredConstructors())
                .filter(constructor -> Arrays.stream(constructor.getParameterTypes())
                        .allMatch(param -> isResolvable(param, knownTypes)))
                .max(Comparator.comparingInt(Constructor::getParameterCount))
                .orElseThrow(() -> new NoSuchMethodException("No usable constructor for " + clazz.getName()));
    }

    private boolean isResolvable(Class<?> paramType, Set<Class<?>> knownTypes) {
        if (knownTypes.contains(paramType)) {
            return true;
        }
        if (List.class.isAssignableFrom(paramType)) {
            return true; // 그냥 넘기고, 생성할 때 따로 해석
        }
        if (paramType.isInterface()) {
            return knownTypes.stream().anyMatch(c -> paramType.isAssignableFrom(c));
        }
        return false;
    }
}
