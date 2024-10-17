package config;

import java.util.HashMap;
import java.util.Map;

public class Container {
    private static Container instance;
    private Map<Class<?>, Object> objectMap;

    public Container() {
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

    public void scan(String packageName) throws Exception {
        ComponentScanner componentScanner = new ComponentScanner();
        componentScanner.scan(packageName);
        for (Object component: componentScanner.getComponents()) {
            register(component.getClass(), component);
        }
    }
}
