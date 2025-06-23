package sprout.beans.internal;

import org.junit.jupiter.api.Test;
import sprout.beans.BeanCreationMethod; // BeanCreationMethod enum 필요
import sprout.beans.BeanDefinition;
import sprout.beans.ConstructorBeanDefinition; // 구현체 임포트
import sprout.beans.MethodBeanDefinition;     // 구현체 임포트

import java.lang.reflect.Constructor;
import java.lang.reflect.Method; // Method 임포트 추가
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class BeanGraphTest {

    // --- 테스트를 위한 가상의 클래스들 (기존 유지) ---
    static class IndependentBean { // 의존성 없음
        public IndependentBean() {}
    }

    static class DependentBeanA { // BeanB에 의존
        public DependentBeanA(DependentBeanB b) {}
    }

    static class DependentBeanB { // BeanC에 의존
        public DependentBeanB(DependentBeanC c) {}
    }

    static class DependentBeanC { // 의존성 없음
        public DependentBeanC() {}
    }

    static class MultiDependentBean { // BeanA, BeanB에 의존
        public MultiDependentBean(DependentBeanA a, DependentBeanB b) {}
    }

    interface MyInterface {}
    static class MyImplementation implements MyInterface { // MyInterface 구현
        public MyImplementation() {}
    }
    static class InterfaceDependentBean { // MyInterface에 의존
        public InterfaceDependentBean(MyInterface myInterface) {}
    }

    // 순환 참조 테스트를 위한 클래스들
    static class CircularA {
        public CircularA(CircularB b) {}
    }

    static class CircularB {
        public CircularB(CircularA a) {}
    }

    // @Configuration 및 @Bean 테스트를 위한 가상 클래스
    static class MyConfigClass {
        // 이 클래스 자체는 빈으로 등록됨 (Configuration 프록시 필요 여부는 테스트에서 설정)
        // 그리고 이 메서드가 BeanDefinition으로 변환됨
        public MyConfigClass() {}

        public ServiceBean serviceBean() { // @Bean 메서드를 시뮬레이션
            return new ServiceBean(new RepositoryBean()); // RepositoryBean에 의존
        }

        public RepositoryBean repositoryBean() { // @Bean 메서드를 시뮬레이션
            return new RepositoryBean();
        }
    }

    static class ServiceBean {
        public ServiceBean(RepositoryBean repo) {}
    }

    static class RepositoryBean {
        public RepositoryBean() {}
    }

    // BeanDefinition 헬퍼 메서드 수정
    private Collection<BeanDefinition> createBeanDefinitions(Object... definitions) throws NoSuchMethodException {
        List<BeanDefinition> defs = new ArrayList<>();
        for (Object defObj : definitions) {
            if (defObj instanceof Class) {
                // ConstructorBeanDefinition 생성
                Class<?> clazz = (Class<?>) defObj;
                Constructor<?> ctor = null;
                Class<?>[] dependencies = new Class<?>[0];
                try {
                    ctor = Arrays.stream(clazz.getDeclaredConstructors())
                            .max(Comparator.comparingInt(Constructor::getParameterCount))
                            .orElse(clazz.getDeclaredConstructor());
                    dependencies = ctor.getParameterTypes();
                } catch (NoSuchMethodException e) {
                    if (clazz.getDeclaredConstructors().length > 0) {
                        ctor = clazz.getDeclaredConstructors()[0];
                        dependencies = ctor.getParameterTypes();
                    } else {
                        throw e;
                    }
                }
                ConstructorBeanDefinition cDef = new ConstructorBeanDefinition(
                        generateBeanName(clazz), clazz, ctor, dependencies);
                // Configuration 클래스인 경우, 프록시 필요하다고 가정 (테스트 목적상)
                if (clazz.equals(MyConfigClass.class)) {
                    cDef.setConfigurationClassProxyNeeded(true);
                }
                defs.add(cDef);
            } else if (defObj instanceof Method) {
                // MethodBeanDefinition 생성
                Method method = (Method) defObj;
                Class<?> beanType = method.getReturnType();
                String factoryBeanName = generateBeanName(method.getDeclaringClass()); // 메서드가 선언된 클래스의 빈 이름
                defs.add(new MethodBeanDefinition(
                        generateBeanName(method), beanType, method, factoryBeanName, method.getParameterTypes()));
            } else {
                throw new IllegalArgumentException("Unsupported definition type: " + defObj.getClass().getName());
            }
        }
        return defs;
    }

    // 빈 이름 생성 헬퍼 (ClassPathScanner에서 가져옴)
    private String generateBeanName(Class<?> clazz) {
        String className = clazz.getSimpleName();
        if (className.length() > 0) {
            return Character.toLowerCase(className.charAt(0)) + className.substring(1);
        }
        return className;
    }

    // 빈 이름 생성 헬퍼 (ClassPathScanner에서 가져옴)
    private String generateBeanName(Method method) {
        // @Bean 어노테이션의 value 속성을 시뮬레이션하지 않고 메서드 이름 그대로 사용
        return method.getName();
    }


    // 기존 테스트 케이스 (BeanDefinition 변경에 맞춰 수정)

    @Test
    void topologicallySorted_shouldReturnCorrectOrderForSimpleDependencies() throws NoSuchMethodException {
        // 의존성: C -> B -> A
        // 예상 순서: C, B, A (상대적)
        Collection<BeanDefinition> definitions = createBeanDefinitions(
                DependentBeanA.class,
                DependentBeanB.class,
                DependentBeanC.class,
                IndependentBean.class
        );

        BeanGraph graph = new BeanGraph(definitions);
        List<BeanDefinition> ordered = graph.topologicallySorted();

        System.out.println("--- Simple Dependencies Order ---");
        ordered.forEach(d -> System.out.println("  " + d.getName() + " (" + d.getType().getSimpleName() + ")"));

        assertRelativeOrder(ordered, DependentBeanC.class, DependentBeanB.class);
        assertRelativeOrder(ordered, DependentBeanB.class, DependentBeanA.class);
    }

    @Test
    void topologicallySorted_shouldHandleMultiDependencies() throws NoSuchMethodException {
        // 의존성: C -> B, A(B), MultiDependentBean(A, B)
        // 예상 순서 (상대적): C, B, A, MultiDependentBean
        Collection<BeanDefinition> definitions = createBeanDefinitions(
                MultiDependentBean.class,
                DependentBeanA.class,
                DependentBeanB.class,
                DependentBeanC.class
        );

        BeanGraph graph = new BeanGraph(definitions);
        List<BeanDefinition> ordered = graph.topologicallySorted();

        System.out.println("--- Multi Dependencies Order ---");
        ordered.forEach(d -> System.out.println("  " + d.getName() + " (" + d.getType().getSimpleName() + ")"));

        assertRelativeOrder(ordered, DependentBeanC.class, DependentBeanB.class);
        assertRelativeOrder(ordered, DependentBeanB.class, DependentBeanA.class);
        assertRelativeOrder(ordered, DependentBeanA.class, MultiDependentBean.class);
        assertRelativeOrder(ordered, DependentBeanB.class, MultiDependentBean.class);
    }

    @Test
    void topologicallySorted_shouldHandleInterfaceDependencies() throws NoSuchMethodException {
        // 의존성: MyInterface -> InterfaceDependentBean
        // 예상 순서: MyImplementation, InterfaceDependentBean
        Collection<BeanDefinition> definitions = createBeanDefinitions(
                InterfaceDependentBean.class,
                MyImplementation.class
        );

        BeanGraph graph = new BeanGraph(definitions);
        List<BeanDefinition> ordered = graph.topologicallySorted();

        System.out.println("--- Interface Dependencies Order ---");
        ordered.forEach(d -> System.out.println("  " + d.getName() + " (" + d.getType().getSimpleName() + ")"));

        assertRelativeOrder(ordered, MyImplementation.class, InterfaceDependentBean.class);
    }

    @Test
    void topologicallySorted_shouldThrowExceptionForCircularDependency() throws NoSuchMethodException {
        // A -> B -> A 순환 참조
        Collection<BeanDefinition> definitions = createBeanDefinitions(
                CircularA.class,
                CircularB.class
        );

        BeanGraph graph = new BeanGraph(definitions);

        System.out.println("--- Circular Dependency Test ---");
        assertThrows(BeanGraph.CircularDependencyException.class, graph::topologicallySorted,
                "CircularDependencyException should be thrown for circular dependencies");
    }


    @Test
    void topologicallySorted_shouldHandleConfigurationAndBeanMethodDependencies() throws NoSuchMethodException {
        // 의존성: myConfigClass (팩토리) -> repositoryBean -> serviceBean
        // MyConfigClass는 serviceBean()과 repositoryBean() 메서드의 팩토리 빈
        // serviceBean은 repositoryBean에 의존
        // 예상 순서 (상대적): myConfigClass, repositoryBean, serviceBean
        // MyConfigClass는 다른 @Bean 메서드의 팩토리이므로 먼저 와야 함
        // repositoryBean이 serviceBean보다 먼저 와야 함
        Collection<BeanDefinition> definitions = createBeanDefinitions(
                MyConfigClass.class,
                MyConfigClass.class.getMethod("serviceBean"), // serviceBean 빈 정의 (팩토리 메서드)
                MyConfigClass.class.getMethod("repositoryBean") // repositoryBean 빈 정의 (팩토리 메서드)
        );

        BeanGraph graph = new BeanGraph(definitions);
        List<BeanDefinition> ordered = graph.topologicallySorted();

        System.out.println("--- Configuration and @Bean Dependencies Order ---");
        ordered.forEach(d -> System.out.println("  " + d.getName() + " (" + d.getType().getSimpleName() + ")"));

        // MyConfigClass (팩토리 빈)는 다른 @Bean 빈보다 먼저 와야 함
        assertRelativeOrder(ordered, MyConfigClass.class, RepositoryBean.class);
        assertRelativeOrder(ordered, MyConfigClass.class, ServiceBean.class);
        // RepositoryBean은 ServiceBean보다 먼저 와야 함
        assertRelativeOrder(ordered, RepositoryBean.class, ServiceBean.class);
    }

    static class AmbiguousImpl1 implements MyInterface {}
    static class AmbiguousImpl2 implements MyInterface {}

    @Test
    void topologicallySorted_shouldThrowExceptionForAmbiguousDependencies() throws NoSuchMethodException {
        Collection<BeanDefinition> definitions = createBeanDefinitions(
                InterfaceDependentBean.class, // MyInterface에 의존
                AmbiguousImpl1.class,
                AmbiguousImpl2.class
        );

        System.out.println("--- Ambiguous Dependency Test ---");
        // BeanGraph 생성 시점에서 예외가 발생
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            new BeanGraph(definitions);
        }, "RuntimeException for ambiguous dependency should be thrown");

        assertTrue(exception.getMessage().contains("Ambiguous dependency for type '" + MyInterface.class.getName()));
        assertTrue(exception.getMessage().contains("Found multiple candidates"));
    }


    private void assertRelativeOrder(List<BeanDefinition> orderedList, Class<?> beforeClass, Class<?> afterClass) {
        BeanDefinition beforeDef = findBeanDefinitionByType(orderedList, beforeClass);
        BeanDefinition afterDef = findBeanDefinitionByType(orderedList, afterClass);

        assertNotNull(beforeDef, "BeanDefinition for " + beforeClass.getSimpleName() + " not found in ordered list.");
        assertNotNull(afterDef, "BeanDefinition for " + afterClass.getSimpleName() + " not found in ordered list.");

        int indexOfBefore = orderedList.indexOf(beforeDef);
        int indexOfAfter = orderedList.indexOf(afterDef);

        assertTrue(indexOfBefore < indexOfAfter,
                String.format("%s (%s) should come before %s (%s) but was not. Order: %s",
                        beforeClass.getSimpleName(), beforeDef.getName(), afterClass.getSimpleName(), afterDef.getName(),
                        orderedList.stream().map(d -> d.getName() + "(" + d.getType().getSimpleName() + ")").collect(Collectors.joining(", "))));
    }

    private BeanDefinition findBeanDefinitionByType(List<BeanDefinition> orderedList, Class<?> type) {
        return orderedList.stream()
                .filter(d -> d.getType().equals(type))
                .findFirst()
                .orElse(null);
    }
}