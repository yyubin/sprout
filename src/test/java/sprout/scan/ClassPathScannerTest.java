package sprout.scan;

import app.test.*;
import org.junit.jupiter.api.Test;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import sprout.beans.BeanDefinition;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ClassPathScannerTest {

    private ConfigurationBuilder createConfigBuilder(String basePackage) {
        return new ConfigurationBuilder()
                .addUrls(ClasspathHelper.forPackage(basePackage))
                .addScanners(Scanners.TypesAnnotated, Scanners.SubTypes)
                .addClassLoaders(ClasspathHelper.contextClassLoader(), ClasspathHelper.staticClassLoader());
    }

    @Test
    void scan_shouldDetectDirectComponentClasses() {
        ClassPathScanner scanner = new ClassPathScanner();
        ConfigurationBuilder configBuilder = createConfigBuilder("app.test"); // app.test 패키지 스캔
        Collection<BeanDefinition> defs = scanner.scan(configBuilder);

        List<Class<?>> componentTypes = defs.stream()
                .map(BeanDefinition::type)
                .collect(Collectors.toList());

        System.out.println("Detected Components (Direct): " + componentTypes);
        // SomeComponent는 @Component 직접 붙어있음
        assertTrue(componentTypes.contains(SomeComponent.class));
    }

    @Test
    void scan_shouldDetectMetaAnnotatedComponentClasses() {
        ClassPathScanner scanner = new ClassPathScanner();
        ConfigurationBuilder configBuilder = createConfigBuilder("app.test");
        Collection<BeanDefinition> defs = scanner.scan(configBuilder);

        List<Class<?>> componentTypes = defs.stream()
                .map(BeanDefinition::type)
                .collect(Collectors.toList());

        System.out.println("Detected Components (Meta-Annotated): " + componentTypes);
        // SomeServiceImpl은 @Service 어노테이션을 가지고 있고, @Service는 @Component를 메타 어노테이션으로 가짐
        assertTrue(componentTypes.contains(SomeServiceImpl.class));
    }

    @Test
    void scan_shouldGenerateCorrectBeanDefinitions() {
        ClassPathScanner scanner = new ClassPathScanner();
        ConfigurationBuilder configBuilder = createConfigBuilder("app.test");
        Collection<BeanDefinition> defs = scanner.scan(configBuilder);

        // SomeServiceImpl의 BeanDefinition 확인 (생성자, 의존성)
        BeanDefinition serviceImplDef = defs.stream()
                .filter(d -> d.type().equals(SomeServiceImpl.class))
                .findFirst()
                .orElseThrow(() -> new AssertionError("SomeServiceImpl BeanDefinition not found"));

        // SomeServiceImpl은 기본 생성자이므로 dependencies가 비어있어야 함
        assertEquals(0, serviceImplDef.dependencies().length);
        assertFalse(serviceImplDef.proxyNeeded()); // 프록시가 필요 없는지 확인

        // ServiceWithDependency의 BeanDefinition 확인
        BeanDefinition serviceWithDepDef = defs.stream()
                .filter(d -> d.type().equals(ServiceWithDependency.class))
                .findFirst()
                .orElseThrow(() -> new AssertionError("ServiceWithDependency BeanDefinition not found"));

        // ServiceWithDependency는 SomeService에 의존하므로 dependencies에 SomeService.class가 있어야 함
        assertEquals(1, serviceWithDepDef.dependencies().length);
        assertEquals(SomeService.class, serviceWithDepDef.dependencies()[0]);
    }

    @Test
    void scan_shouldFilterOutNonConcreteClasses() {
        ClassPathScanner scanner = new ClassPathScanner();
        ConfigurationBuilder configBuilder = createConfigBuilder("app.test");
        Collection<BeanDefinition> defs = scanner.scan(configBuilder);

        List<Class<?>> componentTypes = defs.stream()
                .map(BeanDefinition::type)
                .collect(Collectors.toList());

        // 인터페이스는 빈으로 등록되지 않아야 함
        assertFalse(componentTypes.contains(SomeService.class));
        // 어노테이션 타입 자체는 빈으로 등록되지 않아야 함
        assertFalse(componentTypes.contains(sprout.beans.annotation.Service.class));
    }
}