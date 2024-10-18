package config;

import config.annotations.*;
import org.reflections.Reflections;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Collectors;

public class ComponentScanner {

    private List<Object> components = new ArrayList<>();

    public void scan(String basePackage, Class<? extends java.lang.annotation.Annotation> annotationClass, boolean sorted, boolean requiresDependencies) throws Exception {
        Reflections reflections = new Reflections(basePackage);

        Set<Class<?>> componentClasses = new HashSet<>(reflections.getTypesAnnotatedWith(annotationClass));

        List<Class<?>> sortedComponentClasses = sorted ?
                componentClasses.stream()
                        .sorted(Comparator.comparingInt(c -> {
                            Service priority = c.getAnnotation(Service.class);
                            return (priority != null) ? priority.value() : Integer.MAX_VALUE;
                        }))
                        .toList()
                : new ArrayList<>(componentClasses);

        for (Class<?> componentClass : sortedComponentClasses) {
            if (requiresDependencies) {
                Requires requires = componentClass.getAnnotation(Requires.class);
                if (requires != null) {
                    Object[] parameters = new Object[requires.dependsOn().length];

                    for (int i = 0; i < requires.dependsOn().length; i++) {
                        Object dependencyInstance = Container.getInstance().getByName(requires.dependsOn()[i].getName());
                        parameters[i] = dependencyInstance;
                    }

                    Constructor<?> constructor = componentClass.getDeclaredConstructor(getParameterTypes(requires.dependsOn()));
                    Object serviceInstance = constructor.newInstance(parameters);
                    Container.getInstance().register(componentClass, serviceInstance);
                }
            } else {
                Object instance = componentClass.getDeclaredConstructor().newInstance();
                Container.getInstance().register(componentClass, instance);
            }
        }
    }


    public void scan(String basePackage) throws Exception {
        if (PackageName.http_request.getPackageName().equals(basePackage)) {
            scan(basePackage, Component.class, false, false);
            return;
        }
        if (PackageName.repository.getPackageName().equals(basePackage)) {
            scan(basePackage, Repository.class, false, false);
            return;
        }
        if (PackageName.util.getPackageName().equals(basePackage)) {
            scan(basePackage, Component.class, false, false);
            return;
        }
        if (PackageName.controller.getPackageName().equals(basePackage)) {
            scan(basePackage, Controller.class, false, true);
            return;
        }
        if (PackageName.service.getPackageName().equals(basePackage)) {
            scan(basePackage, Service.class, true, true);
            return;
        }
    }

    private Class<?>[] getParameterTypes(Class<?>[] classes) {
        return classes;
    }

    public List<Object> getComponents() {
        return components;
    }

}
