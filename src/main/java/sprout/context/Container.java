package sprout.context;

import app.service.MemberAuthService;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import sprout.aop.MethodProxyHandler;
import sprout.beans.BeanDefinition;
import sprout.beans.internal.BeanGraph;
import sprout.scan.ClassPathScanner;

import java.lang.reflect.Modifier;
import java.util.*;

public class Container {
    private static Container INSTANCE;
    private final Map<Class<?>, Object> singletons = new HashMap<>();
    private final List<PendingListInjection> pendingListInjections = new ArrayList<>();
    private final ClassPathScanner scanner;

    private static class PendingListInjection {
        final Object beanInstance;
        final Class<?> genericType;
        final List<Object> listToPopulate;

        PendingListInjection(Object beanInstance, List<Object> listToPopulate, Class<?> genericType) {
            this.beanInstance = beanInstance;
            this.listToPopulate = listToPopulate;
            this.genericType = genericType;
        }
    }

    private Container() {
        this.scanner = new ClassPathScanner();
    }
    public static synchronized Container getInstance() { return INSTANCE == null ? (INSTANCE = new Container()) : INSTANCE; }

    public void reset() {
        singletons.clear();
        pendingListInjections.clear();
    }

    public void bootstrap(List<String> basePackages) {
        ConfigurationBuilder configBuilder = new ConfigurationBuilder();
        for (String pkg : basePackages) {
            configBuilder.addUrls(ClasspathHelper.forPackage(pkg));
        }
        configBuilder.addScanners(Scanners.TypesAnnotated, Scanners.SubTypes);
        configBuilder.addClassLoaders(ClasspathHelper.contextClassLoader(), ClasspathHelper.staticClassLoader());

        FilterBuilder filter = new FilterBuilder();
        for (String pkg : basePackages) {
            filter.includePackage(pkg);
        }
        configBuilder.filterInputsBy(filter);

        Collection<BeanDefinition> defs = scanner.scan(configBuilder);
        List<BeanDefinition> order = new BeanGraph(defs).topologicallySorted();
        order.forEach(this::instantiatePrimary);
        postProcessListInjections();
    }

    public <T> T get(Class<T> type) {
        Object bean = singletons.get(type);
        if (bean != null) {
            return type.cast(bean);
        }

        if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
            List<Object> candidates = new ArrayList<>();
            for (Object existingBean : singletons.values()) {
                if (type.isAssignableFrom(existingBean.getClass())) {
                    candidates.add(existingBean);
                }
            }

            if (candidates.size() == 1) {
                return type.cast(candidates.get(0));
            } else if (candidates.size() > 1) {
                throw new RuntimeException("No unique bean of type " + type.getName() + " found. Found " + candidates.size() + " candidates.");
            }
        }

        throw new RuntimeException("No bean of type " + type.getName() + " found in the container.");
    }

    public Collection<Object> beans() { return singletons.values(); }

    private void instantiatePrimary(BeanDefinition def) {
        if (singletons.containsKey(def.type())) return;
        System.out.println("instantiating primary: " + def.type().getName());

        try {
            Class<?>[] paramTypes = def.dependencies();
            var params = def.constructor().getParameters();
            Object[] deps = new Object[paramTypes.length];

            for (int i = 0; i < paramTypes.length; i++) {
                Class<?> paramType = paramTypes[i];

                if (List.class.isAssignableFrom(paramType)) {
                    List<Object> emptyList = new ArrayList<>();
                    deps[i] = emptyList;

                    var genericType = (Class<?>) ((java.lang.reflect.ParameterizedType) params[i].getParameterizedType())
                            .getActualTypeArguments()[0];

                    pendingListInjections.add(new PendingListInjection(null, emptyList, genericType));
                    System.out.println("  " + def.type().getName() + " needs List<" + genericType.getName() + ">, added to pending.");

                } else {
                    deps[i] = get(paramType);
                }
            }

            Object bean = def.constructor().newInstance(deps);

            // 프록시
            // if (def.proxyNeeded()) {
            //     bean = MethodProxyHandler.createProxy(bean, get(MemberAuthService.class));
            // }

            register(def.type(), bean);
            for (Class<?> iface : def.type().getInterfaces()) {
                register(iface, bean);
            }

        } catch (Exception e) {
            throw new RuntimeException("Bean instantiation failed for " + def.type().getName(), e);
        }
    }

    private void postProcessListInjections() {
        System.out.println("--- Post-processing List Injections ---");
        for (PendingListInjection pending : pendingListInjections) {
            Set<Object> uniqueBeansForList = new HashSet<>();
            for (Object bean : singletons.values()) {
                if (pending.genericType.isAssignableFrom(bean.getClass())) {
                    uniqueBeansForList.add(bean);
                }
            }

            pending.listToPopulate.clear();
            pending.listToPopulate.addAll(uniqueBeansForList);

            System.out.println("  Populated List<" + pending.genericType.getName() + "> in a bean with " + uniqueBeansForList.size() + " elements.");
        }
    }

    private void register(Class<?> key, Object bean) { singletons.put(key, bean); }
}
