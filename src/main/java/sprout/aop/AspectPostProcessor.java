package sprout.aop;

import net.sf.cglib.proxy.Enhancer;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import sprout.aop.advice.AdviceFactory;
import sprout.aop.advisor.Advisor;
import sprout.aop.advisor.AdvisorRegistry;
import sprout.aop.advisor.Pointcut;
import sprout.aop.advisor.PointcutFactory;
import sprout.aop.annotation.Around;
import sprout.aop.annotation.Aspect;
import sprout.beans.annotation.Component;
import sprout.beans.processor.BeanPostProcessor;
import sprout.context.Container;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class AspectPostProcessor implements BeanPostProcessor {
    private final AdvisorRegistry advisorRegistry;
    private final Container container;
    private final AdviceFactory adviceFactory;
    private final AtomicBoolean initialized = new AtomicBoolean(false); // 초기화 여부 플래그

    private List<String> basePackages; // 스캔할 기본 패키지 목록

    public AspectPostProcessor(AdvisorRegistry advisorRegistry, Container container, AdviceFactory adviceFactory) {
        this.advisorRegistry = advisorRegistry;
        this.container = container;
        this.adviceFactory = adviceFactory;
    }

    public void initialize(List<String> basePackages) {
        System.out.println("Initializing AspectPostProcessor with basePackages: " + basePackages);
        if (initialized.compareAndSet(false, true)) { // 한 번만 초기화되도록 보장
            this.basePackages = basePackages;
            scanAndRegisterAdvisors(); // 초기화 시점에 Advisor 스캔 및 등록
        }
    }

    private void scanAndRegisterAdvisors() {
        if (basePackages == null || basePackages.isEmpty()) {
            System.err.println("Warning: basePackages not set for AspectPostProcessor. No aspects will be scanned.");
            return;
        }

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

        Reflections reflections = new Reflections(configBuilder);
        Set<Class<?>> aspectClasses = reflections.getTypesAnnotatedWith(Aspect.class);

        for (Class<?> aspectClass : aspectClasses) {
            Object aspectInstance = container.get(aspectClass);
            List<Advisor> advisorsForThisAspect = createAdvisorsFromAspect(aspectInstance);
            System.out.println(aspectClass.getName() + " has " + advisorsForThisAspect.size() + " advisors: " + advisorsForThisAspect);
            for (Advisor advisor : advisorsForThisAspect) {
                advisorRegistry.registerAdvisor(advisor);
            }
        }
        System.out.println("advisorRegistry#getAllAdvisors()" + advisorRegistry.getAllAdvisors());
    }

    private List<Advisor> createAdvisorsFromAspect(Object aspectInstance) {
        List<Advisor> advisors = new ArrayList<>();
        Class<?> aspectClass = aspectInstance.getClass();

        for (Method method : aspectClass.getDeclaredMethods()) {
            // 변경: AdviceFactory를 사용하여 Advisor 객체 전체를 가져옴
            Optional<Advisor> advisor = adviceFactory.createAdvisor(aspectInstance, method);
            advisor.ifPresent(advisors::add);
        }
        return advisors;
    }

    @Override
    public Object postProcessAfterInitialization(String beanName, Object bean) {
        Class<?> targetClass = bean.getClass();

        // 모든 메서드를 순회하며 해당 메서드에 적용될 Advisor가 있는지 확인
        boolean needsProxy = false;
        for (Method method : targetClass.getMethods()) {
            if (Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())) {
                if (!advisorRegistry.getApplicableAdvisors(targetClass, method).isEmpty()) {
                    needsProxy = true;
                    break;
                }
            }
        }

        if (needsProxy) {
            System.out.println("Applying AOP proxy to bean: " + beanName + " (" + targetClass.getName() + ")");
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(targetClass);
            enhancer.setCallback(new BeanMethodInterceptor(bean, advisorRegistry));
            return enhancer.create();
        }
        return bean;
    }


}
