package sprout.beans.internal;

import org.junit.jupiter.api.Test;
import sprout.beans.BeanDefinition;

import java.lang.reflect.Constructor;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class BeanGraphTest {

    // 테스트를 위한 가상의 클래스들
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

    private Collection<BeanDefinition> createBeanDefinitions(Class<?>... classes) throws NoSuchMethodException {
        List<BeanDefinition> defs = new ArrayList<>();
        for (Class<?> clazz : classes) {
            Constructor<?> ctor = null;
            Class<?>[] dependencies = new Class<?>[0];
            try {
                // 가장 파라미터가 많은 생성자를 찾으려는 시도 (혹은 기본 생성자)
                ctor = Arrays.stream(clazz.getDeclaredConstructors())
                        .max(Comparator.comparingInt(Constructor::getParameterCount))
                        .orElse(clazz.getDeclaredConstructor());
                dependencies = ctor.getParameterTypes();
            } catch (NoSuchMethodException e) {
                // 기본 생성자가 없으면 파라미터 있는 생성자 중 첫 번째 것을 사용하거나, 테스트 실패
                // 실제 스캐너는 더 복잡한 로직으로 생성자를 찾습니다.
                if (clazz.getDeclaredConstructors().length > 0) {
                    ctor = clazz.getDeclaredConstructors()[0];
                    dependencies = ctor.getParameterTypes();
                } else {
                    throw e;
                }
            }
            defs.add(new BeanDefinition(clazz, ctor, dependencies, false)); // proxyNeeded는 false로 고정
        }
        return defs;
    }

    @Test
    void topologicallySorted_shouldReturnCorrectOrderForSimpleDependencies() throws NoSuchMethodException {
        // 의존성: C -> B -> A
        // 예상 순서: C, B, A
        Collection<BeanDefinition> definitions = createBeanDefinitions(
                DependentBeanA.class,
                DependentBeanB.class,
                DependentBeanC.class,
                IndependentBean.class // 의존성 없는 빈도 포함
        );

        BeanGraph graph = new BeanGraph(definitions);
        List<BeanDefinition> ordered = graph.topologicallySorted();

        System.out.println("Simple Dependencies Order:");
        ordered.forEach(d -> System.out.println("  " + d.type().getSimpleName()));

        // 의존성 없는 빈은 순서에 구애받지 않지만, C, B, A의 상대적 순서는 유지되어야 함
        int indexC = ordered.indexOf(definitions.stream().filter(d -> d.type().equals(DependentBeanC.class)).findFirst().orElse(null));
        int indexB = ordered.indexOf(definitions.stream().filter(d -> d.type().equals(DependentBeanB.class)).findFirst().orElse(null));
        int indexA = ordered.indexOf(definitions.stream().filter(d -> d.type().equals(DependentBeanA.class)).findFirst().orElse(null));

        assertTrue(indexC < indexB, "DependentBeanC should come before DependentBeanB");
        assertTrue(indexB < indexA, "DependentBeanB should come before DependentBeanA");
    }

    @Test
    void topologicallySorted_shouldHandleMultiDependencies() throws NoSuchMethodException {
        // 의존성: C -> B, B -> A, A -> MultiDependentBean
        //       B -> MultiDependentBean
        // 예상 순서 (상대적): C, B, A, MultiDependentBean (혹은 C, B, MultiDependentBean, A)
        // MultiDependentBean은 A와 B 모두에 의존하므로, A와 B가 모두 생성된 뒤에 와야 함
        Collection<BeanDefinition> definitions = createBeanDefinitions(
                MultiDependentBean.class,
                DependentBeanA.class,
                DependentBeanB.class,
                DependentBeanC.class
        );

        BeanGraph graph = new BeanGraph(definitions);
        List<BeanDefinition> ordered = graph.topologicallySorted();

        System.out.println("Multi Dependencies Order:");
        ordered.forEach(d -> System.out.println("  " + d.type().getSimpleName()));

        int indexC = ordered.indexOf(definitions.stream().filter(d -> d.type().equals(DependentBeanC.class)).findFirst().orElse(null));
        int indexB = ordered.indexOf(definitions.stream().filter(d -> d.type().equals(DependentBeanB.class)).findFirst().orElse(null));
        int indexA = ordered.indexOf(definitions.stream().filter(d -> d.type().equals(DependentBeanA.class)).findFirst().orElse(null));
        int indexMulti = ordered.indexOf(definitions.stream().filter(d -> d.type().equals(MultiDependentBean.class)).findFirst().orElse(null));

        assertTrue(indexC < indexB, "DependentBeanC should come before DependentBeanB");
        assertTrue(indexB < indexA || indexB < indexMulti, "DependentBeanB should come before DependentBeanA or MultiDependentBean");
        assertTrue(indexA < indexMulti, "DependentBeanA should come before MultiDependentBean");
        assertTrue(indexB < indexMulti, "DependentBeanB should come before MultiDependentBean"); // MultiDependentBean은 A, B 모두 뒤에 와야 함
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

        System.out.println("Interface Dependencies Order:");
        ordered.forEach(d -> System.out.println("  " + d.type().getSimpleName()));

        // MyImplementation은 MyInterface를 구현하므로, MyImplementation이 InterfaceDependentBean보다 먼저 와야 함
        int indexImpl = ordered.indexOf(definitions.stream().filter(d -> d.type().equals(MyImplementation.class)).findFirst().orElse(null));
        int indexDep = ordered.indexOf(definitions.stream().filter(d -> d.type().equals(InterfaceDependentBean.class)).findFirst().orElse(null));

        assertTrue(indexImpl < indexDep, "MyImplementation should come before InterfaceDependentBean");
    }

    @Test
    void topologicallySorted_shouldThrowExceptionForCircularDependency() throws NoSuchMethodException {
        // A -> B -> A 순환 참조
        Collection<BeanDefinition> definitions = createBeanDefinitions(
                CircularA.class,
                CircularB.class
        );

        BeanGraph graph = new BeanGraph(definitions);

        System.out.println("Circular Dependency Test:");
        // CircularDependencyException이 발생하는지 확인
        assertThrows(BeanGraph.CircularDependencyException.class, () -> {
            graph.topologicallySorted();
        }, "CircularDependencyException should be thrown for circular dependencies");
    }

}