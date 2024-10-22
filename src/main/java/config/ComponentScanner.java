package config;

import config.annotations.*;
import config.proxy.MethodProxyHandler;
import org.reflections.Reflections;
import service.BoardService;
import service.MemberAuthService;
import service.interfaces.BoardServiceInterface;
import service.interfaces.MemberAuthServiceInterface;

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

            if (componentClass.getAnnotation(BeforeAuthCheck.class) != null) {
                instance = MethodProxyHandler.createProxy(instance, Container.getInstance().get(MemberAuthService.class));
            }

            registerInterface(componentClass, instance);

            Container.getInstance().register(componentClass, instance);
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
