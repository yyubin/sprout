package sprout.scan;

import app.test.*;
import org.junit.jupiter.api.Test;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import sprout.beans.BeanCreationMethod;
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
                .addClassLoaders(ClasspathHelper.contextClassLoader(),
                        ClasspathHelper.staticClassLoader());
    }

    @Test
    void scan_shouldDetectDirectComponentClasses() {
        ClassPathScanner scanner = new ClassPathScanner();
        Collection<BeanDefinition> defs =
                scanner.scan(createConfigBuilder("app.test"));

        List<Class<?>> componentTypes = defs.stream()
                .map(BeanDefinition::getType)
                .collect(Collectors.toList());

        assertTrue(componentTypes.contains(SomeComponent.class),
                "SomeComponent가 스캔되어야 합니다.");
    }

    @Test
    void scan_shouldDetectMetaAnnotatedComponentClasses() {
        ClassPathScanner scanner = new ClassPathScanner();
        Collection<BeanDefinition> defs =
                scanner.scan(createConfigBuilder("app.test"));

        List<Class<?>> componentTypes = defs.stream()
                .map(BeanDefinition::getType)
                .collect(Collectors.toList());

        assertTrue(componentTypes.contains(SomeServiceImpl.class),
                "SomeServiceImpl이 스캔되어야 합니다.");
    }

    @Test
    void scan_shouldGenerateCorrectBeanDefinitions() {
        ClassPathScanner scanner = new ClassPathScanner();
        Collection<BeanDefinition> defs =
                scanner.scan(createConfigBuilder("app.test"));

        // 기본 생성자만 있는 빈
        BeanDefinition serviceImplDef = defs.stream()
                .filter(d -> d.getType().equals(SomeServiceImpl.class))
                .findFirst()
                .orElseThrow(() -> new AssertionError("SomeServiceImpl BeanDefinition not found"));

        assertEquals(BeanCreationMethod.CONSTRUCTOR, serviceImplDef.getCreationMethod());
        assertEquals(0, serviceImplDef.getConstructorArgumentTypes().length,
                "의존성이 없어야 합니다.");
        assertFalse(serviceImplDef.isProxyTarget());

        // 의존성이 하나 있는 빈
        BeanDefinition serviceWithDepDef = defs.stream()
                .filter(d -> d.getType().equals(ServiceWithDependency.class))
                .findFirst()
                .orElseThrow(() -> new AssertionError("ServiceWithDependency BeanDefinition not found"));

        assertEquals(BeanCreationMethod.CONSTRUCTOR, serviceWithDepDef.getCreationMethod());
        assertEquals(1, serviceWithDepDef.getConstructorArgumentTypes().length);
        assertEquals(SomeService.class, serviceWithDepDef.getConstructorArgumentTypes()[0]);
    }

    @Test
    void scan_shouldFilterOutNonConcreteClasses() {
        ClassPathScanner scanner = new ClassPathScanner();
        Collection<BeanDefinition> defs =
                scanner.scan(createConfigBuilder("app.test"));

        List<Class<?>> componentTypes = defs.stream()
                .map(BeanDefinition::getType)
                .collect(Collectors.toList());

        assertFalse(componentTypes.contains(SomeService.class),             // 인터페이스
                "인터페이스는 빈으로 등록되면 안 됩니다.");
        assertFalse(componentTypes.contains(sprout.beans.annotation.Service.class), // 애너테이션 타입
                "애너테이션 자체는 빈으로 등록되면 안 됩니다.");
    }
}
