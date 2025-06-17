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
        final Object beanInstance; // List를 주입받을 빈의 실제 인스턴스
        final Class<?> genericType; // List<T>의 T 타입
        // 생성자 주입 시에는 Parameter 정보를 저장하여 해당 List 객체를 다시 찾아서 채워야 한다
        // 이는 조금 더 복잡합니다. 현재는 생성자에 List 인스턴스가 이미 넘어간 상태이므로,
        // 그 List 인스턴스 자체를 저장해서 나중에 addAll하는게 가장 편할 것 같다
        final List<Object> listToPopulate; // 생성자를 통해 주입된 비어있는 List 인스턴스

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
            filter.includePackage(pkg); // 해당 패키지 또는 하위 패키지에 속하는 클래스만 포함
        }
        configBuilder.filterInputsBy(filter);

        Collection<BeanDefinition> defs = scanner.scan(configBuilder);
        List<BeanDefinition> order = new BeanGraph(defs).topologicallySorted();
        order.forEach(this::instantiatePrimary);
        postProcessListInjections();
    }

    public <T> T get(Class<T> type) {
        // 1. 정확한 타입으로 등록된 빈을 먼저 찾기
        Object bean = singletons.get(type);
        if (bean != null) {
            return type.cast(bean);
        }

        // 2. 인터페이스나 추상 클래스를 요청했을 경우 구현체를 찾기
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
            Set<Object> uniqueBeansForList = new HashSet<>(); // Set을 사용하여 중복 방지
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
