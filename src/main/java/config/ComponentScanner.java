package config;

import config.annotations.Component;
import config.annotations.Controller;
import config.annotations.Repository;
import config.annotations.Service;
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
//
//        List<Class<?>> sortedServiceComponents = reflections.getTypesAnnotatedWith(Service.class).stream()
//                .sorted(Comparator.comparingInt(c -> {
//                    Service priority = c.getAnnotation(Service.class);
//                    return (priority != null) ? priority.value() : Integer.MAX_VALUE;
//                }))
//                .toList();
//
//        for (Class<?> serviceClass : sortedServiceComponents) {
//            Constructor<?> constructor = serviceClass.getDeclaredConstructor();
//            Object serviceInstance;
//
//            Class<?>[] parameterTypes = constructor.getParameterTypes();
//            Object[] parameters = new Object[parameterTypes.length];
//
//            for (int i = 0; i < parameterTypes.length; i++) {
//                Class<?> parameterType = parameterTypes[i];
//
//                parameters[i] = getComponents().stream()
//                        .filter(component -> parameterType.isAssignableFrom(component.getClass()))
//                        .findFirst()
//                        .orElseThrow(() -> new RuntimeException("No component found for " + parameterType.getName())); // parameterType 사용
//            }
//
//            serviceInstance = constructor.newInstance(parameters);
//            components.add(serviceInstance);
//        }
//
//
//        scanComponents.clear();
//        scanComponents.addAll(reflections.getTypesAnnotatedWith(Controller.class));
//
//        for (Class<?> componentClass : scanComponents) {
//            components.add(componentClass.getDeclaredConstructor().newInstance());
//        }
    }

    public List<Object> getComponents() {
        return components;
    }

}
