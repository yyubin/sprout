package config;

import config.annotations.Component;
import config.annotations.Controller;
import org.reflections.Reflections;

import java.io.File;
import java.util.*;

public class ComponentScanner {

    private List<Object> components = new ArrayList<>();

    public void scan(String basePackage) throws Exception {
        Reflections reflections = new Reflections(basePackage);
        Set<Class<?>> scanComponents = reflections.getTypesAnnotatedWith(Component.class);
        for (Class<?> componentClass : scanComponents) {
            components.add(componentClass.getDeclaredConstructor().newInstance());
        }

    }

    public List<Object> getComponents() {
        return components;
    }

}
