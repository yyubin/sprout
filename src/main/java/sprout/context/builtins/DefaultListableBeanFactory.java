package sprout.context.builtins;

import sprout.beans.BeanDefinition;
import sprout.beans.annotation.Order;
import sprout.beans.instantiation.*;
import sprout.beans.matching.BeanTypeMatchingService;
import sprout.beans.processor.BeanPostProcessor;
import sprout.context.*;

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

    // 전략 패턴 적용
    private final List<BeanInstantiationStrategy> instantiationStrategies;
    private final DependencyResolver dependencyResolver;
    private final BeanTypeMatchingService typeMatchingService;

    public DefaultListableBeanFactory() {
        registerCoreSingleton("beanFactory", this);

        // 타입 매칭 서비스 초기화
        this.typeMatchingService = new BeanTypeMatchingService(beanDefinitions, singletons);

        // 의존성 해결 전략 초기화 (체인 순서가 중요)
        List<DependencyTypeResolver> typeResolvers = new ArrayList<>();
        typeResolvers.add(new ListBeanDependencyResolver(pendingListInjections));
        typeResolvers.add(new SingleBeanDependencyResolver(this));
        this.dependencyResolver = new CompositeDependencyResolver(typeResolvers);

        // 인스턴스화 전략 초기화
        this.instantiationStrategies = new ArrayList<>();
        this.instantiationStrategies.add(new ConstructorBasedInstantiationStrategy());
        this.instantiationStrategies.add(new FactoryMethodBasedInstantiationStrategy());
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
        return typeMatchingService.findCandidateNamesForType(type);
    }

    private String choosePrimary(Class<?> requiredType, Set<String> candidates) {
        return typeMatchingService.choosePrimary(requiredType, candidates, primaryTypeToNameMap);
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

    public Object createBean(BeanDefinition def) {
        if (singletons.containsKey(def.getName())) return singletons.get(def.getName());
        System.out.println("instantiating primary: " + def.getType().getName());

        try {
            // 적절한 인스턴스화 전략 선택
            BeanInstantiationStrategy strategy = findStrategy(def);
            if (strategy == null) {
                throw new IllegalArgumentException("No BeanInstantiationStrategy found for creation method: " + def.getCreationMethod());
            }

            // 전략을 사용하여 빈 생성
            Object beanInstance = strategy.instantiate(def, dependencyResolver, this);

            // 생성자 메타 정보 캐싱
            Class<?>[] argTypes = def.getCreationMethod() == sprout.beans.BeanCreationMethod.CONSTRUCTOR
                    ? def.getConstructorArgumentTypes()
                    : def.getFactoryMethodArgumentTypes();
            ctorCache.put(beanInstance, new CtorMeta(argTypes, new Object[0])); // deps는 나중에 필요시 수정

            // BeanPostProcessor 처리
            Object processedBean = beanInstance;
            for (BeanPostProcessor processor : beanPostProcessors) {
                Object result = processor.postProcessBeforeInitialization(def.getName(), processedBean);
                if (result != null) processedBean = result;
            }
            for (BeanPostProcessor processor : beanPostProcessors) {
                Object result = processor.postProcessAfterInitialization(def.getName(), processedBean);
                if (result != null) processedBean = result;
            }

            // 싱글톤 등록
            registerInternal(def.getName(), processedBean);
            return processedBean;

        } catch (Exception e) {
            throw new RuntimeException("Bean instantiation failed for " + def.getType().getName(), e);
        }
    }

    // 빈 정의에 맞는 인스턴스화 전략 찾기
    private BeanInstantiationStrategy findStrategy(BeanDefinition def) {
        for (BeanInstantiationStrategy strategy : instantiationStrategies) {
            if (strategy.supports(def.getCreationMethod())) {
                return strategy;
            }
        }
        return null;
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

    public void postProcessListInjections() {
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
