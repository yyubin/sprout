package sprout.context;

import app.service.MemberAuthService;
import sprout.aop.MethodProxyHandler;
import sprout.beans.BeanDefinition;
import sprout.beans.internal.BeanGraph;
import sprout.scan.ClassPathScanner;

import java.util.*;

public class Container {
    private static Container INSTANCE;
    private final Map<Class<?>, Object> singletons = new HashMap<>();

    private Container() {}
    public static synchronized Container getInstance() { return INSTANCE == null ? (INSTANCE = new Container()) : INSTANCE; }

    // Bootstrap entire context
    public void bootstrap(String basePackage) {
        Collection<BeanDefinition> defs = new ClassPathScanner().scan(basePackage);
        List<BeanDefinition> order = new BeanGraph(defs).topologicallySorted();
        order.forEach(this::instantiate);
    }

    public <T> T get(Class<T> type) { return type.cast(singletons.get(type)); }
    public Collection<Object> beans() { return singletons.values(); }

    private void instantiate(BeanDefinition def) {
        if (singletons.containsKey(def.type())) return;

        try {
            Class<?>[] paramTypes = def.dependencies();
            var params = def.constructor().getParameters();
            Object[] deps = new Object[paramTypes.length];

            for (int i = 0; i < paramTypes.length; i++) {
                Class<?> paramType = paramTypes[i];

                if (List.class.isAssignableFrom(paramType)) {
                    // 제네릭 타입 추출 (ex. List<ArgumentResolver>)
                    var genericType = (Class<?>) ((java.lang.reflect.ParameterizedType) params[i].getParameterizedType())
                            .getActualTypeArguments()[0];

                    // 해당 타입 구현체 전부 모으기
                    List<Object> candidates = new ArrayList<>();
                    for (Object bean : singletons.values()) {
                        if (genericType.isAssignableFrom(bean.getClass())) {
                            candidates.add(bean);
                        }
                    }

                    deps[i] = candidates;
                } else {
                    deps[i] = get(paramType);
                }
            }

            Object bean = def.constructor().newInstance(deps);
            if (def.proxyNeeded()) {
                bean = MethodProxyHandler.createProxy(bean, get(MemberAuthService.class));
            }

            register(def.type(), bean);
            for (Class<?> iface : def.type().getInterfaces()) {
                register(iface, bean);
            }

        } catch (Exception e) {
            throw new RuntimeException("Bean instantiation failed for " + def.type(), e);
        }
    }


    private void register(Class<?> key, Object bean) { singletons.put(key, bean); }
}
