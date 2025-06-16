package sprout.scan;

import sprout.aop.annotation.BeforeAuthCheck;
import sprout.beans.BeanDefinition;
import org.reflections.Reflections;
import sprout.beans.annotation.*;

import java.lang.reflect.Constructor;
import java.util.*;

@Component
public class ClassPathScanner {
    public Collection<BeanDefinition> scan(String basePackage) {
        Reflections r = new Reflections(basePackage);
        Set<Class<?>> cands = new HashSet<>();
        cands.addAll(r.getTypesAnnotatedWith(Component.class));
        List<BeanDefinition> defs = new ArrayList<>(cands.size());
        for (Class<?> c : cands) {
            try {
                Constructor<?> ctor = resolveConstructor(c);
                boolean proxy = c.isAnnotationPresent(BeforeAuthCheck.class);
                defs.add(new BeanDefinition(c, ctor, ctor.getParameterTypes(), proxy));
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("No usable constructor for " + c, e);
            }
        }
        return defs;
    }
    private Constructor<?> resolveConstructor(Class<?> c) throws NoSuchMethodException {
        return Arrays.stream(c.getDeclaredConstructors())
                .max(Comparator.comparingInt(Constructor::getParameterCount))
                .orElse(c.getDeclaredConstructor());
    }
}
