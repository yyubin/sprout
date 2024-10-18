package config;

import config.annotations.*;
import org.reflections.Reflections;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Collectors;

public class ComponentScanner {

    private List<Object> components = new ArrayList<>();

    public void scan(String basePackage) throws Exception {
        Reflections reflections = new Reflections(basePackage);
        Set<Class<?>> scanComponents = new HashSet<>();

        scanComponents.addAll(reflections.getTypesAnnotatedWith(Repository.class));
        scanComponents.addAll(reflections.getTypesAnnotatedWith(Component.class));

        for (Class<?> componentClass : scanComponents) {
            components.add(componentClass.getDeclaredConstructor().newInstance());
        }
        List<Class<?>> sortedServiceComponents = reflections.getTypesAnnotatedWith(Service.class).stream()
                .sorted(Comparator.comparingInt(c -> {
                    Service priority = c.getAnnotation(Service.class);
                    return (priority != null) ? priority.value() : Integer.MAX_VALUE;
                }))
                .toList();

        for (Class<?> serviceClass : sortedServiceComponents) {
            Requires requires = serviceClass.getAnnotation(Requires.class);
            if (requires != null) {
                Object[] parameters = new Object[requires.dependsOn().length];
                for (int i = 0; i < requires.dependsOn().length; i++) {
                    parameters[i] = Container.getInstance().get(requires.dependsOn()[i]);
                }

                Constructor<?> constructor = serviceClass.getDeclaredConstructor(getParameterTypes(requires.dependsOn()));
                Object serviceInstance = constructor.newInstance(parameters);
                components.add(serviceInstance);
            }
        }

        scanComponents.clear();
        scanComponents.addAll(reflections.getTypesAnnotatedWith(Controller.class));

        for (Class<?> componentClass : scanComponents) {
            components.add(componentClass.getDeclaredConstructor().newInstance());
        }


    }

    private Class<?>[] getParameterTypes(Class<?>[] classes) {
        return classes;
    }

    public List<Object> getComponents() {
        return components;
    }

}
