package config;

import controller.ControllerInterface;

import java.util.*;

public class Container {
    private static Container instance;
    private Map<Class<?>, Object> objectMap;

    private Container() {
        objectMap = new HashMap<>();
    }

    public static synchronized Container getInstance() {
        if (instance == null) {
            instance = new Container();
        }
        return instance;
    }

    public <T> void register(Class<?> clazz, Object instance) {
        objectMap.put(clazz, instance);
    }

    public <T> T get(Class<T> clazz) {
        return clazz.cast(objectMap.get(clazz));
    }

    public Object getByType(Class<?> type) {
        for (Map.Entry<Class<?>, Object> entry : objectMap.entrySet()) {
            if (type.isAssignableFrom(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public Object getByName(String className) {
        for (Class<?> clazz : objectMap.keySet()) {
            if (clazz.getName().equals(className)) {
                return objectMap.get(clazz);
            }
        }
        return null;
    }

    public void scan(String packageName) throws Exception {
        ComponentScanner componentScanner = new ComponentScanner();
        componentScanner.scan(packageName);
    }

    public List<ControllerInterface> scanControllers() {
        List<ControllerInterface> controllers = new ArrayList<>();

        for (Map.Entry<Class<?>, Object> entry : objectMap.entrySet()) {
            Class<?> clazz = entry.getKey();
            Object instance = entry.getValue();

            if (ControllerInterface.class.isAssignableFrom(clazz)) {
                controllers.add((ControllerInterface) instance);
            }
        }

        return controllers;
    }

    public Collection<Object> getComponents() {
        return objectMap.values();
    }
}
