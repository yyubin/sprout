package sprout.scan;

import sprout.aop.annotation.BeforeAuthCheck;
import sprout.beans.BeanDefinition;
import org.reflections.Reflections;
import sprout.beans.annotation.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.*;

@Component
public class ClassPathScanner {
    public Collection<BeanDefinition> scan(String basePackage) {
        Reflections r = new Reflections(basePackage);

        Set<Class<?>> candidates = new HashSet<>(r.getTypesAnnotatedWith(Component.class));
        Set<Class<?>> knownTypes = new HashSet<>(candidates);

        List<BeanDefinition> definitions = new ArrayList<>();

        for (Class<?> clazz : candidates) {
            if (clazz.isInterface() || clazz.isAnnotation() || Modifier.isAbstract(clazz.getModifiers())) {
                continue;
            }

            try {
                Constructor<?> ctor = resolveUsableConstructor(clazz, knownTypes);
                boolean proxy = clazz.isAnnotationPresent(BeforeAuthCheck.class);
                definitions.add(new BeanDefinition(clazz, ctor, ctor.getParameterTypes(), proxy));
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("No usable constructor for class " + clazz.getName(), e);
            }
        }

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
