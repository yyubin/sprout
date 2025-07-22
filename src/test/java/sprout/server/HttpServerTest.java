package sprout.server;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HttpServerTest {

    @Test
    @DisplayName("start는 ServerStrategy.start(port)를 위임 호출해야 한다")
    void start_shouldDelegateToStrategy() throws Exception {
        ServerStrategy strategy = Mockito.mock(ServerStrategy.class);
        HttpServer server = new HttpServer(strategy);

        int port = 8080;
        server.start(port);

        verify(strategy).start(port);
        verifyNoMoreInteractions(strategy);
    }

    @Test
    @DisplayName("ServerStrategy.start에서 던진 예외를 그대로 전파해야 한다")
    void start_shouldPropagateException() throws Exception {
        ServerStrategy strategy = Mockito.mock(ServerStrategy.class);
        doThrow(new Exception("boom")).when(strategy).start(anyInt());
        HttpServer server = new HttpServer(strategy);

        assertThrows(Exception.class, () -> server.start(8080));
    }
}
