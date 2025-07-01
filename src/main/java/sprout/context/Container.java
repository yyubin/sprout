package sprout.context;

import net.sf.cglib.proxy.Enhancer;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import sprout.aop.AspectPostProcessor;
import sprout.aop.advisor.Advisor;
import sprout.aop.advisor.AdvisorRegistry;
import sprout.aop.advisor.DefaultPointcutFactory;
import sprout.aop.advisor.PointcutFactory;
import sprout.aop.annotation.Aspect;
import sprout.beans.BeanCreationMethod;
import sprout.beans.BeanDefinition;
import sprout.beans.ConstructorBeanDefinition;
import sprout.beans.annotation.Component;
import sprout.beans.internal.BeanGraph;
import sprout.beans.processor.BeanPostProcessor;
import sprout.scan.ClassPathScanner;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

@Component
public class Container {
    private static Container INSTANCE;
    private final Map<String, Object> singletons = new HashMap<>();
    private final List<PendingListInjection> pendingListInjections = new ArrayList<>();
    private final ClassPathScanner scanner;

    private final Map<Class<?>, String> primaryTypeToNameMap = new HashMap<>(); // 기본 빈 매핑
    private final Map<Class<?>, Set<String>> typeToNamesMap = new HashMap<>(); // 모든 빈 매핑 (동일 타입 여러 개 처리)

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
        primaryTypeToNameMap.clear();
        typeToNamesMap.clear();
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

        registerSingletonInstance("container", this);

        Collection<BeanDefinition> defs = scanner.scan(configBuilder);
        defs.add(new ConstructorBeanDefinition("container", Container.class, null, new Class[0]));
        List<BeanDefinition> order = new BeanGraph(defs).topologicallySorted();

        order.forEach(this::instantiatePrimary);
        postProcessListInjections();

        AspectPostProcessor aspectPostProcessor = get(AspectPostProcessor.class);
        if (aspectPostProcessor != null) {
            aspectPostProcessor.initialize(basePackages);
        }

        applyBeanPostProcessors();
    }

    private void registerSingletonInstance(String name, Object instance) {
        if (singletons.containsKey(name)) {
            throw new RuntimeException("Bean '" + name + "' already registered.");
        }
        singletons.put(name, instance);

        Class<?> type = instance.getClass();
        primaryTypeToNameMap.putIfAbsent(type, name);
        typeToNamesMap.computeIfAbsent(type, k -> new HashSet<>()).add(name);

        for (Class<?> iface : type.getInterfaces()) {
            primaryTypeToNameMap.putIfAbsent(iface, name);
            typeToNamesMap.computeIfAbsent(iface, k -> new HashSet<>()).add(name);
        }
        for (Class<?> p = type.getSuperclass();
             p != null && p != Object.class;
             p = p.getSuperclass()) {
            primaryTypeToNameMap.putIfAbsent(p, name);
            typeToNamesMap.computeIfAbsent(p, k -> new HashSet<>()).add(name);
        }
    }

    public <T> T get(Class<T> type) {
        String primaryName = primaryTypeToNameMap.get(type);
        if (primaryName != null) {
            return type.cast(singletons.get(primaryName));
        }

        Set<String> candidateNames = typeToNamesMap.get(type);
        if (candidateNames == null || candidateNames.isEmpty()) {
            throw new RuntimeException("No bean of type " + type.getName() + " found in the container.");
        }

        if (candidateNames.size() == 1) {
            return type.cast(singletons.get(candidateNames.iterator().next()));
        } else {
            throw new RuntimeException("No unique bean of type " + type.getName() + " found. Found " + candidateNames.size() + " candidates: " + candidateNames);
        }
    }

    public <T> T get(String name) {
        Object bean = singletons.get(name);
        if (bean == null) {
            throw new RuntimeException("No bean with name '" + name + "' found in the container.");
        }
        return (T) bean;
    }

    public <T> List<T> getAll(Class<T> type) {
        Set<String> candidateNames = typeToNamesMap.get(type);
        if (candidateNames == null || candidateNames.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> beans = new ArrayList<>();
        for (String name : candidateNames) {
            beans.add(type.cast(singletons.get(name)));
        }
        return beans;
    }

    public Collection<Object> beans() { return singletons.values(); }

    private void instantiatePrimary(BeanDefinition def) {
        if (singletons.containsKey(def.getName())) return;
        System.out.println("instantiating primary: " + def.getType().getName());

        Object beanInstance;
        try {
            Object[] deps;
            Parameter[] methodParams;
            if (def.getCreationMethod() == BeanCreationMethod.CONSTRUCTOR) {
                Constructor<?> constructor = def.getConstructor();
                methodParams = constructor.getParameters();
                deps = resolveDependencies(def.getConstructorArgumentTypes(), methodParams, def.getType());
                if (def.isConfigurationClassProxyNeeded()) {
                    Enhancer enhancer = new Enhancer();
                    enhancer.setSuperclass(def.getType());
                    enhancer.setCallback(new ConfigurationMethodInterceptor(this));
                    beanInstance = enhancer.create(def.getConstructorArgumentTypes(), deps);
                } else {
                    beanInstance = def.getConstructor().newInstance(deps);
                }
            } else if (def.getCreationMethod() == BeanCreationMethod.FACTORY_METHOD) {
                Object factoryBean = get(def.getFactoryBeanName());
                Method factoryMethod = def.getFactoryMethod();
                methodParams = factoryMethod.getParameters();
                deps = resolveDependencies(def.getFactoryMethodArgumentTypes(), methodParams, def.getType());
                beanInstance = def.getFactoryMethod().invoke(factoryBean, deps);
            } else {
                throw new IllegalArgumentException("Unsupported bean creation method: " + def.getCreationMethod());
            }

            // 프록시
            // if (def.proxyNeeded()) {
            //     bean = MethodProxyHandler.createProxy(bean, get(MemberAuthService.class));
            // }

            registerInternal(def.getName(), beanInstance);

        } catch (Exception e) {
            throw new RuntimeException("Bean instantiation failed for " + def.getType().getName(), e);
        }
    }

    private Object[] resolveDependencies(Class<?>[] dependencyTypes, Parameter[] params, Class<?> currentBeanType) {
        Object[] deps = new Object[dependencyTypes.length];
        for (int i = 0; i < dependencyTypes.length; i++) {
            Class<?> paramType = dependencyTypes[i];
            if (List.class.isAssignableFrom(paramType)) {
                List<Object> emptyList = new ArrayList<>();
                deps[i] = emptyList;

                var genericType = (Class<?>) ((java.lang.reflect.ParameterizedType) params[i].getParameterizedType())
                        .getActualTypeArguments()[0];

                pendingListInjections.add(new PendingListInjection(null, emptyList, genericType));
            } else {
                deps[i] = get(paramType); // 단일 빈 의존성 주입
            }
        }
        return deps;
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

    private void registerInternal(String name, Object bean) {
        if (singletons.containsKey(name)) return;        // 이미 있으면 무시

        singletons.put(name, bean);

        Class<?> type = bean.getClass();
        primaryTypeToNameMap.putIfAbsent(type, name);
        typeToNamesMap.computeIfAbsent(type, k -> new HashSet<>()).add(name);

        for (Class<?> iface : type.getInterfaces()) {
            primaryTypeToNameMap.putIfAbsent(iface, name);
            typeToNamesMap.computeIfAbsent(iface, k -> new HashSet<>()).add(name);
        }
        for (Class<?> p = type.getSuperclass();
             p != null && p != Object.class;
             p = p.getSuperclass()) {
            primaryTypeToNameMap.putIfAbsent(p, name);
            typeToNamesMap.computeIfAbsent(p, k -> new HashSet<>()).add(name);
        }
    }

    private void applyBeanPostProcessors() {
        // 등록된 모든 빈 후처리기를 가져옴
        List<BeanPostProcessor> postProcessors = getAll(BeanPostProcessor.class);

        // 모든 싱글톤 빈에 대해 후처리기 적용
        List<String> beanNames = new ArrayList<>(singletons.keySet()); // ConcurrentModificationException 방지
        for (String beanName : beanNames) {
            Object originalBean = singletons.get(beanName);
            Object processedBean = originalBean;

            for (BeanPostProcessor processor : postProcessors) {
                processedBean = processor.postProcessBeforeInitialization(beanName, processedBean);
            }

            // 초기화 후 처리 (프록시 적용)
            for (BeanPostProcessor processor : postProcessors) {
                processedBean = processor.postProcessAfterInitialization(beanName, processedBean);
            }

            // 만약 프록시가 생성되었다면, 기존 빈을 프록시로 교체
            if (processedBean != originalBean) {
                singletons.put(beanName, processedBean); // 맵의 값을 프록시로 업데이트
                // typeToNamesMap 등의 매핑도 업데이트 필요
                updateBeanMappings(beanName, originalBean.getClass(), processedBean.getClass());
            }
        }
    }

    private void updateBeanMappings(String name, Class<?> originalType, Class<?> newType) {
        // 1. originalType에 대한 매핑 제거 (name이 originalType의 유일한 기본 빈이 아닌 경우)
        // primaryTypeToNameMap에서 해당 name이 originalType에 대한 primary 매핑이었으면 제거
        if (primaryTypeToNameMap.get(originalType) != null && primaryTypeToNameMap.get(originalType).equals(name)) {
            primaryTypeToNameMap.remove(originalType);
        }

        // typeToNamesMap에서 originalType에 해당하는 name 제거
        Set<String> originalNamesForType = typeToNamesMap.get(originalType);
        if (originalNamesForType != null) {
            originalNamesForType.remove(name);
            if (originalNamesForType.isEmpty()) {
                typeToNamesMap.remove(originalType);
            }
        }

        primaryTypeToNameMap.putIfAbsent(newType, name);
        // 새로운 타입(프록시 타입)을 해당 빈의 모든 타입 매핑에 추가
        typeToNamesMap.computeIfAbsent(newType, k -> new HashSet<>()).add(name);

        for (Class<?> iface : newType.getInterfaces()) {
            primaryTypeToNameMap.putIfAbsent(iface, name);
            typeToNamesMap.computeIfAbsent(iface, k -> new HashSet<>()).add(name);
        }
        // 프록시의 상위 클래스들에 대해 매핑 업데이트
        for (Class<?> p = newType.getSuperclass();
             p != null && p != Object.class;
             p = p.getSuperclass()) {
            primaryTypeToNameMap.putIfAbsent(p, name);
            typeToNamesMap.computeIfAbsent(p, k -> new HashSet<>()).add(name);
        }

        // 참고: 프록시는 원본 빈의 인터페이스/상속 계층을 유지하므로
        // 원본 타입이 가지고 있던 인터페이스나 상위 클래스 매핑은
        // 프록시 타입이 이들을 구현/상속하므로 자연스럽게 유지
        // 다만, primaryTypeToNameMap에서 originalType이 primary였던 경우
        // newType이 그 자리를 대체하게 되므로, primaryTypeToNameMap에서 originalType을 제거하는 로직이 필요
    }

    public void registerRuntimeBean(String name, Object bean) {
        registerInternal(name, bean);
    }

    public boolean containsBean(String name) {
        return singletons.containsKey(name);
    }
}
