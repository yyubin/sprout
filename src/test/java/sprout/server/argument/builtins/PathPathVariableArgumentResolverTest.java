package sprout.server.argument.builtins;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sprout.mvc.annotation.PathVariable;
import sprout.server.websocket.InvocationContext;

import java.lang.reflect.Parameter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PathPathVariableArgumentResolverTest {

    @Mock private InvocationContext mockContext;

    @InjectMocks
    private PathPathVariableArgumentResolver resolver;

    private static class DummyHandler {
        // @PathVariable("id") String userId
        void handle(@PathVariable("id") String userId, String noAnnotation) {}
    }

    @Test
    @DisplayName("@PathVariable이 있고 pathVars가 존재할 때 supports가 true를 반환해야 한다.")
    void supports_shouldReturnTrue_forPathVariableAndContextWithPathVars() throws NoSuchMethodException {
        // given
        Parameter param = DummyHandler.class.getDeclaredMethod("handle", String.class, String.class).getParameters()[0];
        when(mockContext.pathVars()).thenReturn(Map.of("id", "123"));

        // when
        boolean result = resolver.supports(param, mockContext);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("@PathVariable이 없으면 supports가 false를 반환해야 한다.")
    void supports_shouldReturnFalse_whenNoPathVariableAnnotation() throws NoSuchMethodException {
        // given
        Parameter param = DummyHandler.class.getDeclaredMethod("handle", String.class, String.class).getParameters()[1];
        lenient().when(mockContext.pathVars()).thenReturn(Map.of("id", "123"));

        // when
        boolean result = resolver.supports(param, mockContext);

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("resolve는 pathVars에서 올바른 값을 변환하여 반환해야 한다.")
    void resolve_shouldReturnValueFromPathVars() throws Exception {
        // given
        Parameter param = DummyHandler.class.getDeclaredMethod("handle", String.class, String.class).getParameters()[0];
        when(mockContext.pathVars()).thenReturn(Map.of("id", "test-user"));

        // when
        Object result = resolver.resolve(param, mockContext);

        // then
        assertEquals("test-user", result);
    }
}