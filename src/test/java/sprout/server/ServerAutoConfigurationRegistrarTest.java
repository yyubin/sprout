package sprout.server;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import sprout.beans.BeanDefinition;
import sprout.beans.ConstructorBeanDefinition;
import sprout.config.AppConfig;
import sprout.context.ContextPropagator;
import sprout.mvc.dispatcher.RequestDispatcher;
import sprout.mvc.http.parser.HttpRequestParser;
import sprout.server.builtins.BioHttpProtocolHandler;
import sprout.server.builtins.NioHttpProtocolHandler;
import sprout.server.builtins.RequestExecutorPoolService;
import sprout.server.builtins.VirtualRequestExecutorService;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.*;

@DisplayName("ServerAutoConfigurationRegistrar (수정된 로직) 테스트")
class ServerAutoConfigurationRegistrarTest {

    private AppConfig mockAppConfig;
    private ServerAutoConfigurationRegistrar registrar;

    @BeforeEach
    void setUp() {
        mockAppConfig = mock(AppConfig.class);
        registrar = new ServerAutoConfigurationRegistrar(mockAppConfig);
    }

    // 4가지 설정 조합을 제공하는 static 메서드
    static Stream<Arguments> configurationProvider() {
        return Stream.of(
                arguments("hybrid", "virtual", BioHttpProtocolHandler.class, VirtualRequestExecutorService.class),
                arguments("hybrid", "platform", BioHttpProtocolHandler.class, RequestExecutorPoolService.class),
                arguments("nio", "virtual", NioHttpProtocolHandler.class, VirtualRequestExecutorService.class),
                arguments("nio", "platform", NioHttpProtocolHandler.class, RequestExecutorPoolService.class)
        );
    }

    @ParameterizedTest(name = "실행모드: {0}, 스레드타입: {1}")
    @MethodSource("configurationProvider")
    @DisplayName("다양한 설정 조합에 따라 올바른 빈들을 등록해야 한다")
    void register_withVariousConfigs_shouldRegisterCorrectBeans(
            String executionMode, String threadType,
            Class<?> expectedHttpHandlerClass, Class<?> expectedExecutorClass) throws Exception {

        // given
        // 1. Mock AppConfig 설정
        when(mockAppConfig.getStringProperty("server.execution-mode", "hybrid")).thenReturn(executionMode);
        when(mockAppConfig.getStringProperty("server.thread-type", "virtual")).thenReturn(threadType);
        when(mockAppConfig.getIntProperty("server.thread-pool-size", 100)).thenReturn(50); // 플랫폼 스레드 테스트용

        // 2. 가상 스레드 테스트를 위한 기존 빈(ContextPropagator) 설정
        BeanDefinition mockPropagatorDef = createMockPropagatorDefinition();
        List<BeanDefinition> existingDefs = List.of(mockPropagatorDef);


        // when
        Collection<BeanDefinition> definitions = registrar.registerAdditionalBeanDefinitions(existingDefs);


        // then
        // 1. HttpProtocolHandler 빈 검증
        BeanDefinition httpHandlerDef = findBeanByName(definitions, "httpProtocolHandler");
        assertThat(httpHandlerDef.getType()).isEqualTo(expectedHttpHandlerClass);
        assertThat(httpHandlerDef).isInstanceOf(ConstructorBeanDefinition.class);

        // 2. RequestExecutorService 빈 검증
        BeanDefinition executorDef = findBeanByName(definitions, "requestExecutorService");
        assertThat(executorDef.getType()).isEqualTo(expectedExecutorClass);
        assertThat(executorDef).isInstanceOf(ConstructorBeanDefinition.class);
        ConstructorBeanDefinition ctorExecutorDef = (ConstructorBeanDefinition) executorDef;

        if (expectedExecutorClass.equals(VirtualRequestExecutorService.class)) {
            Constructor<?> expectedCtor = VirtualRequestExecutorService.class.getConstructor(List.class);
            assertThat(ctorExecutorDef.getConstructor()).isEqualTo(expectedCtor);
            // 참고: 생성자 '인자'에 대한 검증은 아래 '코드 설명' 참고
        } else {
            Constructor<?> expectedCtor = RequestExecutorPoolService.class.getConstructor(int.class);
            assertThat(ctorExecutorDef.getConstructor()).isEqualTo(expectedCtor);
            assertThat(ctorExecutorDef.getConstructorArguments()).containsExactly(50);
        }
    }

    private BeanDefinition findBeanByName(Collection<BeanDefinition> definitions, String name) {
        return definitions.stream()
                .filter(def -> def.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Bean with name '" + name + "' not found."));
    }

    // ContextPropagator 타입으로 인식될 수 있는 Mock BeanDefinition 생성
    private BeanDefinition createMockPropagatorDefinition() {
        // 중요: 현재 SUT 코드의 버그로 인해 이 Mock은 실제로는 필터링되지 않습니다.
        // 테스트는 SUT의 '의도'를 기반으로 작성되었습니다.
        BeanDefinition mockDef = mock(BeanDefinition.class);
        // 의도된 동작이라면, getBeanClass()를 통해 타입을 확인해야 합니다.
        doReturn(SomeContextPropagator.class).when(mockDef).getType();
        return mockDef;
    }

    // 테스트용 ContextPropagator 구현체
    private static class SomeContextPropagator implements ContextPropagator<Object> {
        @Override public Object capture() { return null; }
        @Override public void restore(Object value) { }
        @Override public void clear() { }
    }
}