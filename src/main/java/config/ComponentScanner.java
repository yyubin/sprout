package config;

import config.annotations.*;
import sprout.aop.MethodProxyHandler;
import sprout.mvc.mapping.ControllerInterface;
import sprout.mvc.annotation.DeleteMapping;
import sprout.mvc.annotation.GetMapping;
import sprout.mvc.annotation.PostMapping;
import sprout.mvc.annotation.PutMapping;
import sprout.mvc.http.HttpMethod;
import sprout.aop.annotation.BeforeAuthCheck;
import sprout.beans.annotation.*;
import sprout.mvc.mapping.RequestMappingRegistry;
import org.reflections.Reflections;
import app.service.MemberAuthService;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

public class ComponentScanner {

    private final RequestMappingRegistry requestMappingRegistry = new RequestMappingRegistry();

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

            if (componentClass.getAnnotation(BeforeAuthCheck.class) != null) {
                instance = MethodProxyHandler.createProxy(instance, Container.getInstance().get(MemberAuthService.class));
            }

            registerInterface(componentClass, instance);

            Container.getInstance().register(componentClass, instance);
        }
    }

    private void registerControllerMappings(ControllerInterface controller) throws Exception {
        Class<?> controllerClass = controller.getClass();
        for (Method method : controllerClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(GetMapping.class)) {
                String path = method.getAnnotation(GetMapping.class).path();
                requestMappingRegistry.register(path, HttpMethod.GET, controller, method);
            } else if (method.isAnnotationPresent(PostMapping.class)) {
                String path = method.getAnnotation(PostMapping.class).path();
                requestMappingRegistry.register(path, HttpMethod.POST, controller, method);
            } else if (method.isAnnotationPresent(PutMapping.class)) {
                String path = method.getAnnotation(PutMapping.class).path();
                requestMappingRegistry.register(path, HttpMethod.PUT, controller, method);
            } else if (method.isAnnotationPresent(DeleteMapping.class)) {
                String path = method.getAnnotation(DeleteMapping.class).path();
                requestMappingRegistry.register(path, HttpMethod.DELETE, controller, method);
            }
        }
    }

    private void registerInterface(Class<?> componentClass, Object instance) {
        for (Class<?> iface : componentClass.getInterfaces()) {
            Container.getInstance().register(iface, instance);
        }
    }

    private Object[] resolveDependencies(Class<?>[] dependencies) {
        Object[] parameters = new Object[dependencies.length];
        for (int i = 0; i < dependencies.length; i++) {
            parameters[i] = Container.getInstance().getByType(dependencies[i]);
        }
        return parameters;
    }

    private Class<?>[] getParameterTypes(Class<?>[] classes) {
        return classes;
    }

}
