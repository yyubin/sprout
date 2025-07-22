package sprout.security.context;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import sprout.security.core.SecurityContext;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class SecurityContextPropagatorTest {

    @Test
    @DisplayName("capture()는 SecurityContextHolder.getContext() 반환값을 그대로 돌려준다")
    void capture_returnsHolderContext() {
        SecurityContext ctx = mock(SecurityContext.class);

        try (MockedStatic<SecurityContextHolder> holder = mockStatic(SecurityContextHolder.class)) {
            holder.when(SecurityContextHolder::getContext).thenReturn(ctx);

            SecurityContextPropagator propagator = new SecurityContextPropagator();
            SecurityContext captured = propagator.capture();

            assertSame(ctx, captured);
            holder.verify(SecurityContextHolder::getContext);
        }
    }

    @Test
    @DisplayName("restore(ctx)는 SecurityContextHolder.setContext(ctx)를 호출한다")
    void restore_setsHolder() {
        SecurityContext ctx = mock(SecurityContext.class);

        try (MockedStatic<SecurityContextHolder> holder = mockStatic(SecurityContextHolder.class)) {
            SecurityContextPropagator propagator = new SecurityContextPropagator();
            propagator.restore(ctx);

            holder.verify(() -> SecurityContextHolder.setContext(ctx));
        }
    }

    @Test
    @DisplayName("clear()는 SecurityContextHolder.clearContext()를 호출한다")
    void clear_callsClearOnHolder() {
        try (MockedStatic<SecurityContextHolder> holder = mockStatic(SecurityContextHolder.class)) {
            SecurityContextPropagator propagator = new SecurityContextPropagator();
            propagator.clear();

            holder.verify(SecurityContextHolder::clearContext);
        }
    }
}
