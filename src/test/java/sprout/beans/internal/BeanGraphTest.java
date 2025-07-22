package sprout.beans.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sprout.beans.BeanCreationMethod;
import sprout.beans.BeanDefinition;
import sprout.beans.ConstructorBeanDefinition;
import sprout.beans.MethodBeanDefinition;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BeanGraphTest {

    // --- 테스트를 위한 가상의 클래스들 ---
    static class IndependentBean { public IndependentBean() {} }

    static class DependentBeanC { public DependentBeanC() {} }
    static class DependentBeanB { public DependentBeanB(DependentBeanC c) {} }
    static class DependentBeanA { public DependentBeanA(DependentBeanB b) {} }

    static class MultiDependentBean { public MultiDependentBean(DependentBeanA a, DependentBeanB b) {} }

    interface MyInterface {}
    static class MyImplementation implements MyInterface { public MyImplementation() {} }
    static class AnotherImplementation implements MyInterface { public AnotherImplementation() {} }
    static class InterfaceDependentBean { public InterfaceDependentBean(MyInterface myInterface) {} }

    static class CircularA { public CircularA(CircularB b) {} }
    static class CircularB { public CircularB(CircularA a) {} }

    static class MyConfigClass {
        public MyConfigClass() {}
        public ServiceBean serviceBean(RepositoryBean repo) { return new ServiceBean(repo); }
        public RepositoryBean repositoryBean() { return new RepositoryBean(); }
    }
    static class ServiceBean { public ServiceBean(RepositoryBean repo) {} }
    static class RepositoryBean { public RepositoryBean() {} }

    // --- 헬퍼 메서드 ---
    private ConstructorBeanDefinition ctorDef(String name, Class<?> type) throws NoSuchMethodException {
        Constructor<?> ctor = type.getDeclaredConstructors()[0];
        return new ConstructorBeanDefinition(name, type, ctor, ctor.getParameterTypes());
    }

    private MethodBeanDefinition factoryDef(String name,
                                            Class<?> returnType,
                                            Class<?> configType,
                                            String factoryBeanName,
                                            String methodName,
                                            Class<?>... paramTypes) throws NoSuchMethodException {
        Method m = configType.getDeclaredMethod(methodName, paramTypes);
        return new MethodBeanDefinition(name, returnType, m, factoryBeanName, m.getParameterTypes());
    }

    @Nested
    @DisplayName("토폴로지컬 정렬 기본 케이스")
    class TopologicalSortCases {

        @Test
        @DisplayName("단순 선형 의존: C -> B -> A 순서로 정렬되어야 한다")
        void linearDependencies() throws Exception {
            BeanDefinition c = ctorDef("beanC", DependentBeanC.class);
            BeanDefinition b = ctorDef("beanB", DependentBeanB.class);
            BeanDefinition a = ctorDef("beanA", DependentBeanA.class);

            BeanGraph graph = new BeanGraph(List.of(a, b, c)); // 순서 섞여 들어가도 됨
            List<BeanDefinition> sorted = graph.topologicallySorted();

            int idxC = sorted.indexOf(c);
            int idxB = sorted.indexOf(b);
            int idxA = sorted.indexOf(a);

            assertTrue(idxC < idxB && idxB < idxA, "C < B < A 순서여야 합니다.");
        }

        @Test
        @DisplayName("여러 독립 빈 + 다중 의존 빈도 올바르게 정렬된다")
        void multiDependencies() throws Exception {
            BeanDefinition indep = ctorDef("independent", IndependentBean.class);
            BeanDefinition c = ctorDef("beanC", DependentBeanC.class);
            BeanDefinition b = ctorDef("beanB", DependentBeanB.class);
            BeanDefinition a = ctorDef("beanA", DependentBeanA.class);
            BeanDefinition multi = ctorDef("multi", MultiDependentBean.class);

            BeanGraph graph = new BeanGraph(List.of(multi, indep, a, b, c));
            List<BeanDefinition> sorted = graph.topologicallySorted();

            int iIndep = sorted.indexOf(indep);
            int iC = sorted.indexOf(c);
            int iB = sorted.indexOf(b);
            int iA = sorted.indexOf(a);
            int iMulti = sorted.indexOf(multi);

            assertTrue(iC < iB && iB < iA && iA < iMulti, "C < B < A < Multi 순서여야 합니다.");
            // IndependentBean은 어디 있어도 상관없지만, 사이클 없이 포함되어야 함
            assertTrue(iIndep >= 0, "IndependentBean도 정렬 결과에 포함되어야 합니다.");
        }

        @Test
        @DisplayName("인터페이스 의존성은 구현체 하나만 있을 때 해결되어야 한다")
        void interfaceResolution_singleCandidate() throws Exception {
            BeanDefinition impl = ctorDef("impl", MyImplementation.class);
            BeanDefinition ifaceDep = ctorDef("ifaceDep", InterfaceDependentBean.class);

            BeanGraph graph = new BeanGraph(List.of(impl, ifaceDep));
            List<BeanDefinition> sorted = graph.topologicallySorted();

            assertTrue(sorted.indexOf(impl) < sorted.indexOf(ifaceDep),
                    "구현체가 인터페이스 의존 빈보다 먼저 와야 합니다.");
        }

        @Test
        @DisplayName("순환 참조가 있으면 CircularDependencyException이 발생해야 한다")
        void circularDependency() throws Exception {
            BeanDefinition a = ctorDef("circularA", CircularA.class);
            BeanDefinition b = ctorDef("circularB", CircularB.class);

            BeanGraph graph = new BeanGraph(List.of(a, b));

            assertThrows(BeanGraph.CircularDependencyException.class, graph::topologicallySorted);
        }
    }

    @Nested
    @DisplayName("에러/엣지 케이스")
    class ErrorCases {

        @Test
        @DisplayName("인터페이스 구현체가 여러 개면 모호성 오류를 던져야 한다")
        void ambiguousDependency() throws Exception {
            BeanDefinition impl1 = ctorDef("impl1", MyImplementation.class);
            BeanDefinition impl2 = ctorDef("impl2", AnotherImplementation.class);
            BeanDefinition ifaceDep = ctorDef("ifaceDep", InterfaceDependentBean.class);

            // buildEdges() 단계에서 바로 RuntimeException 발생
            assertThrows(RuntimeException.class, () -> new BeanGraph(List.of(impl1, impl2, ifaceDep)));
        }

        @Test
        @DisplayName("팩토리 메서드 빈 의존성: config -> factoryBean, repo -> service 순서 보장")
        void factoryMethodBeans() throws Exception {
            // 구성
            BeanDefinition configDef = ctorDef("myConfigClass", MyConfigClass.class);

            BeanDefinition repoDef = factoryDef(
                    "repositoryBean",
                    RepositoryBean.class,
                    MyConfigClass.class,
                    "myConfigClass",
                    "repositoryBean"
            );

            BeanDefinition serviceDef = factoryDef(
                    "serviceBean",
                    ServiceBean.class,
                    MyConfigClass.class,
                    "myConfigClass",
                    "serviceBean",
                    RepositoryBean.class
            );

            BeanGraph graph = new BeanGraph(List.of(configDef, repoDef, serviceDef));
            List<BeanDefinition> sorted = graph.topologicallySorted();

            int iConfig = sorted.indexOf(configDef);
            int iRepo   = sorted.indexOf(repoDef);
            int iServ   = sorted.indexOf(serviceDef);

            assertTrue(iConfig < iRepo, "Config 빈이 repositoryBean보다 먼저 생성되어야 함");
            assertTrue(iRepo < iServ, "repositoryBean이 serviceBean보다 먼저 생성되어야 함");

            // BeanDefinition들이 FACTORY_METHOD/CONSTRUCTOR로 세팅됐는지 간단 확인
            assertEquals(BeanCreationMethod.FACTORY_METHOD, repoDef.getCreationMethod());
            assertEquals(BeanCreationMethod.FACTORY_METHOD, serviceDef.getCreationMethod());
            assertEquals(BeanCreationMethod.CONSTRUCTOR, configDef.getCreationMethod());
        }
    }
}
