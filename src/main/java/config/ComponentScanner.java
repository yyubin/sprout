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
        if (PackageName.repository.getPackageName().equals(basePackage)) {
            scanRepositoryPackage(basePackage);
            return;
        }

        if (PackageName.util.getPackageName().equals(basePackage)) {
            scanUtilPackage(basePackage);
            return;
        }

        if (PackageName.controller.getPackageName().equals(basePackage)) {
            scanControllerPackage(basePackage);
            return;
        }

        if (PackageName.service.getPackageName().equals(basePackage)) {
            scanServicePackage(basePackage);
            return;
        }
    }

    public void scanControllerPackage(String basePackage) throws Exception {
        Reflections reflections = new Reflections(basePackage);
        Set<Class<?>> scanComponents = new HashSet<>(reflections.getTypesAnnotatedWith(Controller.class));
        for (Class<?> componentClass : scanComponents) {
            components.add(componentClass.getDeclaredConstructor().newInstance());
        }
    }

    public void scanRepositoryPackage(String basePackage) throws Exception {
        Reflections reflections = new Reflections(basePackage);
        Set<Class<?>> scanComponents = new HashSet<>(reflections.getTypesAnnotatedWith(Repository.class));
        for (Class<?> componentClass : scanComponents) {
            components.add(componentClass.getDeclaredConstructor().newInstance());
        }
    }

    public void scanServicePackage(String basePackage) throws Exception {
        Reflections reflections = new Reflections(basePackage);
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
                    Object dependencyInstance = Container.getInstance().getByName(requires.dependsOn()[i].getName());
                    parameters[i] = dependencyInstance;
                }

                Constructor<?> constructor = serviceClass.getDeclaredConstructor(getParameterTypes(requires.dependsOn()));
                Object serviceInstance = constructor.newInstance(parameters);
                Container.getInstance().register(serviceClass, serviceInstance);
            }
        }
    }

    public void scanUtilPackage(String basePackage) throws Exception {
        Reflections reflections = new Reflections(basePackage);
        Set<Class<?>> scanComponents = new HashSet<>(reflections.getTypesAnnotatedWith(Component.class));
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
