package config;

import config.annotations.*;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.*;

public class ComponentScanner {

    public void scan(String basePackage) throws Exception {
        Reflections reflections = new Reflections(basePackage);

        Set<Class<?>> componentClasses = new HashSet<>();
        componentClasses.addAll(reflections.getTypesAnnotatedWith(Component.class));
        componentClasses.addAll(reflections.getTypesAnnotatedWith(Controller.class));
        componentClasses.addAll(reflections.getTypesAnnotatedWith(Service.class));
        componentClasses.addAll(reflections.getTypesAnnotatedWith(Repository.class));

        List<Class<?>> sortedComponentClasses = componentClasses.stream()
                .sorted(Comparator.comparingInt(c -> {
                    Priority priority = c.getAnnotation(Priority.class);
                    return (priority != null) ? priority.value() : Integer.MAX_VALUE;
                }))
                .toList();

        for (Class<?> componentClass : sortedComponentClasses) {
            Requires requires = componentClass.getAnnotation(Requires.class);
            Object instance;

            if (requires != null) {
                Object[] parameters = resolveDependencies(requires.dependsOn());
                Constructor<?> constructor = componentClass.getDeclaredConstructor(getParameterTypes(requires.dependsOn()));
                instance = constructor.newInstance(parameters);
            } else {
                instance = componentClass.getDeclaredConstructor().newInstance();
            }

            Container.getInstance().register(componentClass, instance);
        }
    }

    private Object[] resolveDependencies(Class<?>[] dependencies) {
        Object[] parameters = new Object[dependencies.length];
        for (int i = 0; i < dependencies.length; i++) {
            parameters[i] = Container.getInstance().getByName(dependencies[i].getName());
        }
        return parameters;
    }

    private Class<?>[] getParameterTypes(Class<?>[] classes) {
        return classes;
    }

}
