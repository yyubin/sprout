package sprout.context.builtins;

import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import sprout.aop.annotation.Aspect;
import sprout.beans.BeanDefinition;
import sprout.beans.InfrastructureBean;
import sprout.beans.annotation.*;
import sprout.beans.processor.BeanDefinitionRegistrar;
import sprout.beans.processor.BeanPostProcessor;
import sprout.context.*;
import sprout.context.lifecycle.*;
import sprout.mvc.advice.annotation.ControllerAdvice;
import sprout.scan.ClassPathScanner;
import sprout.server.websocket.annotation.WebSocketHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SproutApplicationContext implements ApplicationContext {

    private final DefaultListableBeanFactory beanFactory;
    private final List<String> basePackages;
    private final ClassPathScanner scanner;
    private final BeanLifecycleManager lifecycleManager;

    private List<BeanDefinition> infraDefs;
    private List<BeanDefinition> appDefs;

    public SproutApplicationContext(String... basePackages) {
        this.beanFactory = new DefaultListableBeanFactory();
        this.basePackages = List.of(basePackages);
        this.scanner = new ClassPathScanner();
        this.beanFactory.registerCoreSingleton("applicationContext", this);

        // BeanLifecycleManager 초기화
        List<BeanLifecyclePhase> phases = new ArrayList<>();
        phases.add(new InfrastructureBeanPhase());
        phases.add(new BeanPostProcessorRegistrationPhase());
        phases.add(new ApplicationBeanPhase());
        phases.add(new ContextInitializerPhase());
        this.lifecycleManager = new BeanLifecycleManager(phases);
    }

    private void scanBeanDefinitions() throws NoSuchMethodException {
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

        beanFactory.registerSingletonInstance("container", this);

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

        this.infraDefs = infraDefs;
        this.appDefs = appDefs;
    }

    @Override
    public void refresh() throws Exception {
        // 1. 빈 정의 스캔
        scanBeanDefinitions();

        // 2. BeanLifecycleManager를 통한 단계별 실행
        BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
                beanFactory,
                infraDefs,
                appDefs,
                basePackages
        );

        lifecycleManager.executePhases(context);
    }

    @Override
    public void close() {
        beanFactory.reset();
    }

    @Override
    public void reset() {
        beanFactory.reset();
    }

    @Override
    public Object getBean(String name) {
        return beanFactory.getBean(name);
    }

    @Override
    public <T> T getBean(Class<T> requiredType) {
        return beanFactory.getBean(requiredType);
    }

    @Override
    public Collection<Object> getAllBeans() {
        return beanFactory.getAllBeans();
    }

    @Override
    public <T> List<T> getAllBeans(Class<T> type) {
        return beanFactory.getAllBeans(type);
    }

    @Override
    public boolean containsBean(String name) {
        return beanFactory.containsBean(name);
    }

    @Override
    public void registerRuntimeBean(String name, Object bean) {
        beanFactory.registerSingletonInstance(name, bean);
    }

    @Override
    public void registerCoreSingleton(String name, Object bean) {
        beanFactory.registerCoreSingleton(name, bean);
    }

    @Override
    public CtorMeta lookupCtorMeta(Object bean) {
        return beanFactory.lookupCtorMeta(bean);
    }
}
