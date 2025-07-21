package sprout.context;

import net.sf.cglib.proxy.Enhancer;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import sprout.aop.AspectPostProcessor;
import sprout.aop.annotation.Aspect;
import sprout.beans.BeanCreationMethod;
import sprout.beans.BeanDefinition;
import sprout.beans.ConstructorBeanDefinition;
import sprout.beans.InfrastructureBean;
import sprout.beans.annotation.*;
import sprout.beans.internal.BeanGraph;
import sprout.beans.processor.BeanDefinitionRegistrar;
import sprout.beans.processor.BeanPostProcessor;
import sprout.mvc.advice.annotation.ControllerAdvice;
import sprout.scan.ClassPathScanner;
import sprout.server.websocket.annotation.WebSocketHandler;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

import static java.util.stream.Collectors.toList;

@Component
public class Container {
    private static Container INSTANCE;
    private final Map<String, Object> singletons = new HashMap<>();
    private final List<PendingListInjection> pendingListInjections = new ArrayList<>();
    private final ClassPathScanner scanner;

    private final Map<Class<?>, String> primaryTypeToNameMap = new HashMap<>(); // 기본 빈 매핑
    private final Map<Class<?>, Set<String>> typeToNamesMap = new HashMap<>(); // 모든 빈 매핑 (동일 타입 여러 개 처리)
    private final Map<Object, CtorMeta> ctorCache = new IdentityHashMap<>();

    private List<BeanPostProcessor> beanPostProcessors;

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
        beanPostProcessors = null;
    }

    public void bootstrap(List<String> basePackages) throws NoSuchMethodException {
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

        Collection<BeanDefinition> allDefs = scanner.scan(configBuilder,
                Component.class,
                Controller.class,
                Service.class,
                Repository.class,
                Configuration.class,
                Aspect.class,
                ControllerAdvice.class,
                WebSocketHandler.class
        );

        List<BeanDefinitionRegistrar> registrars = allDefs.stream()
                .filter(bd -> BeanDefinitionRegistrar.class.isAssignableFrom(bd.getType()))
                .map(bd -> {
                    try {
                        return (BeanDefinitionRegistrar) bd.getType().getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to instantiate BeanDefinitionRegistrar: " + bd.getType().getName(), e);
                    }
                })
                .toList();

        for (BeanDefinitionRegistrar registrar : registrars) {
            allDefs.addAll(registrar.registerAdditionalBeanDefinitions(allDefs));
        }

        List<BeanDefinition> infraDefs = new ArrayList<>(allDefs.stream()
                .filter(bd -> BeanPostProcessor.class.isAssignableFrom(bd.getType()) ||
                        InfrastructureBean.class.isAssignableFrom(bd.getType()))
                .toList());

        List<BeanDefinition> appDefs = new ArrayList<>(allDefs);
        appDefs.removeAll(infraDefs);

        // 2-phased initial
        beanPostProcessors = new ArrayList<>();
        instantiateGroup(infraDefs, false, basePackages);

        beanPostProcessors = getAll(BeanPostProcessor.class);
        instantiateGroup(appDefs, true, basePackages);
    }

    private void instantiateGroup(List<BeanDefinition> defs, boolean isApp, List<String> basePackages) {
        List<BeanDefinition> order = new BeanGraph(defs).topologicallySorted();

        if (isApp) {
            AspectPostProcessor registeredAspectPostProcessor = get(AspectPostProcessor.class);
            if (registeredAspectPostProcessor != null) {
                registeredAspectPostProcessor.initialize(basePackages);
            }
        }

        order.forEach(this::instantiatePrimary);
        postProcessListInjections();
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

                if (def instanceof ConstructorBeanDefinition && ((ConstructorBeanDefinition) def).getConstructorArguments() != null) {
                    deps = ((ConstructorBeanDefinition) def).getConstructorArguments();
                } else {
                    deps = resolveDependencies(def.getConstructorArgumentTypes(), methodParams, def.getType());
                }
                if (def.isConfigurationClassProxyNeeded()) {
                    Enhancer enhancer = new Enhancer();
                    enhancer.setSuperclass(def.getType());
                    //enhancer.setCallback(new ConfigurationMethodInterceptor(this));
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

            ctorCache.put(beanInstance, new CtorMeta(def.getConstructorArgumentTypes(), deps));

            Object processedBean = beanInstance; // 처리될 빈 인스턴스 (초기에는 원본)

            // postProcessBeforeInitialization 적용
            for (BeanPostProcessor processor : beanPostProcessors) {
                processedBean = processor.postProcessBeforeInitialization(def.getName(), processedBean);
            }

            // postProcessAfterInitialization 적용 (AOP 프록시 생성 로직 포함)
            // AspectPostProcessor의 postProcessAfterInitialization에서 프록시가 생성되어 반환될 수 있음
            for (BeanPostProcessor processor : beanPostProcessors) {
                processedBean = processor.postProcessAfterInitialization(def.getName(), processedBean);
            }

            registerInternal(def.getName(), processedBean);

        } catch (Exception e) {
            throw new RuntimeException("Bean instantiation failed for " + def.getType().getName(), e);
        }
    }

    public CtorMeta lookupCtorMeta(Object bean) {
        return ctorCache.get(bean);
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

            List<Object> sortedBeansForList = uniqueBeansForList.stream()
                    .sorted(Comparator.comparingInt(bean -> {
                        Class<?> clazz = bean.getClass();
                        Order order = clazz.getAnnotation(Order.class);
                        return (order != null) ? order.value() : Integer.MAX_VALUE;
                    }))
                    .toList();

            pending.listToPopulate.clear();
            pending.listToPopulate.addAll(sortedBeansForList);

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

    public void registerRuntimeBean(String name, Object bean) {
        registerInternal(name, bean);
    }

    public boolean containsBean(String name) {
        return singletons.containsKey(name);
    }
}
