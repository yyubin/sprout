package sprout.boot;

import sprout.beans.annotation.ComponentScan;
import sprout.context.Container;
import sprout.mvc.advice.ControllerAdviceRegistry;
import sprout.mvc.mapping.HandlerMethodScanner;
import sprout.server.HttpServer;
import sprout.server.websocket.endpoint.WebSocketHandlerScanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class SproutApplication {

    public static void run(Class<?> primarySource) throws Exception {
        List<String> packages = getPackagesToScan(primarySource);

        Container ctx = Container.getInstance();
        ctx.bootstrap(packages);

        HandlerMethodScanner handlerMethodScanner = ctx.get(HandlerMethodScanner.class);
        handlerMethodScanner.scanControllers();

        ControllerAdviceRegistry controllerAdviceRegistry = ctx.get(ControllerAdviceRegistry.class);
        controllerAdviceRegistry.scanControllerAdvices();

        WebSocketHandlerScanner webSocketHandlerScanner = ctx.get(WebSocketHandlerScanner.class);
        webSocketHandlerScanner.scanWebSocketHandlers();

        HttpServer server = ctx.get(HttpServer.class);
        server.start(8080);
    }

    private static List<String> getPackagesToScan(Class<?> primarySource) {
        ComponentScan componentScan = primarySource.getAnnotation(ComponentScan.class);
        if (componentScan != null) {
            List<String> packages = new ArrayList<>();
            packages.addAll(Arrays.asList(componentScan.value()));
            packages.addAll(Arrays.asList(componentScan.basePackages()));
            if (!packages.isEmpty()) {
                return packages;
            }
        }
        return List.of(primarySource.getPackage().getName());
    }
}