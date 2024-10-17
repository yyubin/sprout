package config;

import config.annotations.Component;
import config.annotations.Controller;
import config.annotations.Repository;
import config.annotations.Service;
import org.reflections.Reflections;

import java.io.File;
import java.util.*;

public class ComponentScanner {

    private List<Object> components = new ArrayList<>();

    public void scan(String basePackage) throws Exception {
        Reflections reflections = new Reflections(basePackage);
        Set<Class<?>> scanComponents = reflections.getTypesAnnotatedWith(Controller.class);
        scanComponents.addAll(reflections.getTypesAnnotatedWith(Service.class));
        scanComponents.addAll(reflections.getTypesAnnotatedWith(Repository.class));
        for (Class<?> componentClass : scanComponents) {
            components.add(componentClass.getDeclaredConstructor().newInstance());
        }

    }

    public List<Object> getComponents() {
        return components;
    }

}
