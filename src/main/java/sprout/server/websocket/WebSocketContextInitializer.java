package sprout.server.websocket;

import sprout.beans.annotation.Component;
import sprout.context.ApplicationContext;
import sprout.context.BeanFactory;
import sprout.context.ContextInitializer;
import sprout.server.websocket.endpoint.WebSocketHandlerScanner;

@Component
public class WebSocketContextInitializer implements ContextInitializer {
    private final WebSocketHandlerScanner scanner;

    public WebSocketContextInitializer(WebSocketHandlerScanner scanner) {
        this.scanner = scanner;
    }

    @Override
    public void initializeAfterRefresh(BeanFactory context) {
        scanner.scanWebSocketHandlers(context);
    }
}
