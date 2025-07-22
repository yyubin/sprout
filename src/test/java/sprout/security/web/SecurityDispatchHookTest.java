package sprout.security.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import sprout.mvc.dispatcher.DispatchHook;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.HttpResponse;
import sprout.security.context.SecurityContextHolder;

import static org.mockito.Mockito.*;

class SecurityDispatchHookTest {

    @Test
    @DisplayName("beforeDispatch는 SecurityContextHolder.createEmptyContext()를 호출한다")
    void beforeDispatch_callsCreateEmptyContext() {
        HttpRequest<?> req = mock(HttpRequest.class);
        HttpResponse res = mock(HttpResponse.class);

        try (MockedStatic<SecurityContextHolder> holder = mockStatic(SecurityContextHolder.class)) {
            DispatchHook hook = new SecurityDispatchHook();

            hook.beforeDispatch(req, res);

            holder.verify(SecurityContextHolder::createEmptyContext);
            holder.verifyNoMoreInteractions();
        }
    }

    @Test
    @DisplayName("afterDispatch는 SecurityContextHolder.clearContext()를 호출한다")
    void afterDispatch_callsClearContext() {
        HttpRequest<?> req = mock(HttpRequest.class);
        HttpResponse res = mock(HttpResponse.class);

        try (MockedStatic<SecurityContextHolder> holder = mockStatic(SecurityContextHolder.class)) {
            DispatchHook hook = new SecurityDispatchHook();

            hook.afterDispatch(req, res);

            holder.verify(SecurityContextHolder::clearContext);
            holder.verifyNoMoreInteractions();
        }
    }
}
