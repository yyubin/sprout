package sprout.server.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sprout.context.BeanFactory;
import sprout.server.websocket.endpoint.WebSocketHandlerScanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class WebSocketContextInitializerTest {

    @Mock
    WebSocketHandlerScanner scanner;
    @Mock
    BeanFactory beanFactory;

    WebSocketContextInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new WebSocketContextInitializer(scanner);
    }

    @Test
    @DisplayName("initializeAfterRefresh는 BeanFactory를 Scanner로 전달한다")
    void initialize_delegates_to_scanner() {
        initializer.initializeAfterRefresh(beanFactory);
        verify(scanner).scanWebSocketHandlers(beanFactory);
        verifyNoMoreInteractions(scanner);
    }
}
