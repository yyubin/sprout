package sprout.context.builtins;

import net.sf.cglib.proxy.Enhancer;
import sprout.beans.BeanCreationMethod;
import sprout.beans.BeanDefinition;
import sprout.beans.ConstructorBeanDefinition;
import sprout.beans.annotation.Order;
import sprout.beans.processor.BeanPostProcessor;
import sprout.context.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultListableBeanFactory implements BeanFactory, BeanDefinitionRegistry {

    private final Map<String, BeanDefinition> beanDefinitions = new ConcurrentHashMap<>();
    private final Map<String, Object> singletons = new ConcurrentHashMap<>();
    private final List<PendingListInjection> pendingListInjections = new ArrayList<>();
    private final Map<Class<?>, String> primaryTypeToNameMap = new HashMap<>(); // 기본 빈 매핑
    private final Map<Class<?>, Set<String>> typeToNamesMap = new HashMap<>(); // 모든 빈 매핑 (동일 타입 여러 개 처리)
    private final Map<Object, CtorMeta> ctorCache = new IdentityHashMap<>();

    private List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();

    public DefaultListableBeanFactory() {
        registerCoreSingleton("beanFactory", this);
    }

    @Override
    public void registerCoreSingleton(String name, Object bean) {
        registerSingletonInstance(name, bean);
        primaryTypeToNameMap.put(bean.getClass(), name);
        System.out.println(primaryTypeToNameMap);
        typeToNamesMap.computeIfAbsent(bean.getClass(), k -> new HashSet<>()).add(name);
    }

    @Override
    public void reset() {
        singletons.clear();
        pendingListInjections.clear();
        primaryTypeToNameMap.clear();
        typeToNamesMap.clear();
        beanPostProcessors.clear();
    }

    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) {
        beanDefinitions.put(beanName, beanDefinition);
    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName) {
        return beanDefinitions.get(beanName);
    }

    @Override
    public boolean containsBeanDefinition(String beanName) {
        return beanDefinitions.containsKey(beanName);
    }

    @Override
    public Object getBean(String name) {
        Object singleton = singletons.get(name);
        if (singleton != null) {
            return singleton;
        }

        // singletons 맵에 없으면 BeanDefinition을 보고 생성
        BeanDefinition def = beanDefinitions.get(name);
        if (def == null) {
            throw new RuntimeException("No bean named '" + name + "' is defined.");
        }

        // instantiatePrimary를 createBean으로 변경하고 호출
        return createBean(def);
    }

    @Override
    public <T> T getBean(Class<T> requiredType) {
        // 1) 이미 만들어져 있으면 바로 반환
        T bean = getIfPresent(requiredType);
        if (bean != null) return bean;

        // 2) 후보 BeanDefinition/Singleton 이름 수집
        Set<String> candidates = candidateNamesForType(requiredType);
        if (candidates.isEmpty()) {
            throw new RuntimeException("No bean of type " + requiredType.getName() + " found in the container.");
        }

        // 3) primary 우선
        String primary = choosePrimary(requiredType, candidates);
        if (primary == null) {
            if (candidates.size() == 1) primary = candidates.iterator().next();
            else throw new RuntimeException("No unique bean of type " + requiredType.getName() +
                    " found. Candidates: " + candidates);
        }

        // 4) 필요시 생성 후 반환
        @SuppressWarnings("unchecked")
        T created = (T) createIfNecessary(primary);
        return created;
    }

    private <T> T getIfPresent(Class<T> type) {
        String name = primaryTypeToNameMap.get(type);
        if (name != null) {
            Object singleton = singletons.get(name);
            if (singleton != null && type.isInstance(singleton)) {
                return type.cast(singleton);
            }
        }

        Set<String> names = typeToNamesMap.get(type);
        if (names != null) {
            for (String n : names) {
                Object obj = singletons.get(n); // n은 절대 null 아님
                if (obj != null && type.isInstance(obj)) return type.cast(obj);
            }
        }
        return null;
    }

    private Set<String> candidateNamesForType(Class<?> type) {
        Set<String> names = new HashSet<>();
        // 1) 이미 등록된 싱글턴
        for (Map.Entry<String, Object> e : singletons.entrySet()) {
            if (type.isAssignableFrom(e.getValue().getClass())) names.add(e.getKey());
        }
        // 2) 아직 안만든 정의
        for (Map.Entry<String, BeanDefinition> e : beanDefinitions.entrySet()) {
            if (type.isAssignableFrom(e.getValue().getType())) names.add(e.getKey());
        }
        return names;
    }

    private String choosePrimary(Class<?> requiredType, Set<String> candidates) {
        // BeanDefinition 중 primary=true 인 애만 필터
        List<String> primaries = candidates.stream()
                .filter(name -> {
                    BeanDefinition d = beanDefinitions.get(name);
                    return d != null && d.isPrimary();
                })
                .toList();

        if (primaries.size() == 1) return primaries.get(0);
        if (primaries.size() > 1)
            throw new RuntimeException("@Primary beans conflict for type " + requiredType.getName() + ": " + primaries);

        // 기존 primaryTypeToNameMap fallback
        String mapped = primaryTypeToNameMap.get(requiredType);
        if (mapped != null && candidates.contains(mapped)) return mapped;

        return null;
    }

    private Object createIfNecessary(String name) {
        Object singleton = singletons.get(name);
        if (singleton != null) return singleton;

        BeanDefinition def = beanDefinitions.get(name);
        if (def == null) {
            throw new RuntimeException("No bean named '" + name + "' is defined.");
        }
        return createBean(def);
    }

    @Override
    public <T> List<T> getAllBeans(Class<T> type) {
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

    @Override
    public boolean containsBean(String name) {
        return singletons.containsKey(name);
    }

    @Override
    public void registerRuntimeBean(String name, Object bean) {
        registerInternal(name, bean);
    }

    @Override
    public Collection<Object> getAllBeans() {
        return this.singletons.values();
    }

    @Override
    public CtorMeta lookupCtorMeta(Object bean) {
        return ctorCache.get(bean);
    }

    public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
        if (this.beanPostProcessors == null) {
            this.beanPostProcessors = new ArrayList<>();
        }
        this.beanPostProcessors.add(beanPostProcessor);
    }

    public void preInstantiateSingletons() throws Exception {
        for (String beanName : beanDefinitions.keySet()) {
            getBean(beanName); // getBean 호출을 통해 빈 생성 트리거
        }
    }

    protected Object createBean(BeanDefinition def) {
        if (singletons.containsKey(def.getName())) return singletons.get(def.getName());
        System.out.println("instantiating primary: " + def.getType().getName());

        Object beanInstance;
        try {
            Object[] deps;
            Parameter[] methodParams;
            if (def.getCreationMethod() == BeanCreationMethod.CONSTRUCTOR) {
                Constructor<?> constructor = def.getConstructor();
                methodParams = constructor.getParameters();

                if (def instanceof ConstructorBeanDefinition && ((ConstructorBeanDefinition) def).getConstructorArguments() != null) {
                    deps = ((ConstructorBeanDefinition) def).getConstructorArguments();
                } else {
                    deps = resolveDependencies(def.getConstructorArgumentTypes(), methodParams, def.getType());
                }
                if (def.isConfigurationClassProxyNeeded()) {
                    Enhancer enhancer = new Enhancer();
                    enhancer.setSuperclass(def.getType());
                    enhancer.setCallback(new ConfigurationMethodInterceptor(this));
                    beanInstance = enhancer.create(def.getConstructorArgumentTypes(), deps);
                } else {
                    beanInstance = def.getConstructor().newInstance(deps);
                }
            } else if (def.getCreationMethod() == BeanCreationMethod.FACTORY_METHOD) {
                Object factoryBean = getBean(def.getFactoryBeanName());
                Method factoryMethod = def.getFactoryMethod();
                deps = resolveDependencies(def.getFactoryMethodArgumentTypes(), factoryMethod.getParameters(), def.getType());
                beanInstance = def.getFactoryMethod().invoke(factoryBean, deps);
            } else {
                throw new IllegalArgumentException("Unsupported bean creation method: " + def.getCreationMethod());
            }

            ctorCache.put(beanInstance, new CtorMeta(def.getConstructorArgumentTypes(), deps));

            Object processedBean = beanInstance;
            for (BeanPostProcessor processor : beanPostProcessors) {
                Object result = processor.postProcessBeforeInitialization(def.getName(), processedBean);
                if (result != null) processedBean = result;
            }
            for (BeanPostProcessor processor : beanPostProcessors) {
                Object result = processor.postProcessAfterInitialization(def.getName(), processedBean);
                if (result != null) processedBean = result;
            }

            registerInternal(def.getName(), processedBean);
            return processedBean;

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
                deps[i] = getBean(paramType); // 단일 빈 의존성 주입
            }
        }
        return deps;
    }

    public void registerSingletonInstance(String name, Object instance) {
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

    protected void postProcessListInjections() {
        System.out.println("--- Post-processing List Injections ---");
        for (PendingListInjection pending : pendingListInjections) {
            Set<Object> uniqueBeansForList = new HashSet<>();
            for (Object bean : singletons.values()) {
                if (pending.getGenericType().isAssignableFrom(bean.getClass())) {
                    uniqueBeansForList.add(bean);
                }
            }

            List<Object> sortedBeansForList = uniqueBeansForList.stream()
                    .sorted(Comparator.comparingInt(bean -> {
                        Class<?> clazz = bean.getClass();
                        Order order = clazz.getAnnotation(Order.class);
                        return (order != null) ? order.value() : Integer.MAX_VALUE;
                    }))
                    .toList();

            pending.getListToPopulate().clear();
            pending.getListToPopulate().addAll(sortedBeansForList);

            System.out.println("  Populated List<" + pending.getGenericType().getName() + "> in a bean with " + uniqueBeansForList.size() + " elements.");
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
}
