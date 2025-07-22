package sprout.scan;

import app.test.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import sprout.aop.annotation.Aspect;
import sprout.beans.BeanCreationMethod;
import sprout.beans.BeanDefinition;
import sprout.beans.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ClassPathScannerTest {

    private ConfigurationBuilder createConfigBuilder(String basePackage) {
        return new ConfigurationBuilder()
                .addUrls(ClasspathHelper.forPackage(basePackage))
                .addScanners(Scanners.TypesAnnotated, Scanners.SubTypes)
                .addClassLoaders(ClasspathHelper.contextClassLoader(),
                        ClasspathHelper.staticClassLoader());
    }

    private Collection<BeanDefinition> scan(String basePackage) {
        ClassPathScanner scanner = new ClassPathScanner();
        return scanner.scan(
                createConfigBuilder(basePackage),
                Component.class,
                Service.class,
                Repository.class,
                Controller.class,
                Configuration.class,
                Aspect.class
        );
    }

    @Test
    @DisplayName("@Component 직접 부착 클래스가 스캔되어야 한다")
    void scan_shouldDetectDirectComponentClasses() {
        Collection<BeanDefinition> defs = scan("app.test");

        List<Class<?>> componentTypes = defs.stream()
                .map(BeanDefinition::getType)
                .collect(Collectors.toList());

        assertTrue(componentTypes.contains(SomeComponent.class), "SomeComponent가 스캔되어야 합니다.");
    }

    @Test
    @DisplayName("메타 애노테이션(@Service 등)도 스캔되어야 한다")
    void scan_shouldDetectMetaAnnotatedComponentClasses() {
        Collection<BeanDefinition> defs = scan("app.test");

        List<Class<?>> componentTypes = defs.stream()
                .map(BeanDefinition::getType)
                .collect(Collectors.toList());

        assertTrue(componentTypes.contains(SomeServiceImpl.class), "SomeServiceImpl이 스캔되어야 합니다.");
    }

    @Test
    @DisplayName("생성자 기반 BeanDefinition 정보가 올바르게 생성되어야 한다")
    void scan_shouldGenerateCorrectBeanDefinitions() {
        Collection<BeanDefinition> defs = scan("app.test");

        // 기본 생성자 빈
        BeanDefinition serviceImplDef = defs.stream()
                .filter(d -> d.getType().equals(SomeServiceImpl.class))
                .findFirst()
                .orElseThrow(() -> new AssertionError("SomeServiceImpl BeanDefinition not found"));

        assertEquals(BeanCreationMethod.CONSTRUCTOR, serviceImplDef.getCreationMethod());
        assertEquals(0, serviceImplDef.getConstructorArgumentTypes().length, "의존성이 없어야 합니다.");
        assertFalse(serviceImplDef.isProxyTarget());

        // 의존성 1개 있는 빈
        BeanDefinition serviceWithDepDef = defs.stream()
                .filter(d -> d.getType().equals(ServiceWithDependency.class))
                .findFirst()
                .orElseThrow(() -> new AssertionError("ServiceWithDependency BeanDefinition not found"));

        assertEquals(BeanCreationMethod.CONSTRUCTOR, serviceWithDepDef.getCreationMethod());
        assertEquals(1, serviceWithDepDef.getConstructorArgumentTypes().length);
        assertEquals(SomeService.class, serviceWithDepDef.getConstructorArgumentTypes()[0]);
    }

    @Test
    @DisplayName("인터페이스/애노테이션/추상 클래스는 제외되어야 한다")
    void scan_shouldFilterOutNonConcreteClasses() {
        Collection<BeanDefinition> defs = scan("app.test");

        List<Class<?>> componentTypes = defs.stream()
                .map(BeanDefinition::getType)
                .collect(Collectors.toList());

        assertFalse(componentTypes.contains(SomeService.class), "인터페이스는 빈으로 등록되면 안 됩니다.");
        assertFalse(componentTypes.contains(sprout.beans.annotation.Service.class), "애노테이션 타입은 빈으로 등록되면 안 됩니다.");
    }
}
