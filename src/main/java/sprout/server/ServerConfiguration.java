package sprout.server;

import sprout.beans.annotation.Bean;
import sprout.beans.annotation.Configuration;
import sprout.config.AppConfig;
import sprout.context.BeanFactory;
import sprout.context.ContextPropagator;
import sprout.mvc.dispatcher.RequestDispatcher;
import sprout.mvc.http.parser.HttpRequestParser;
import sprout.server.builtins.BioHttpProtocolHandler;
import sprout.server.builtins.NioHttpProtocolHandler;
import sprout.server.builtins.RequestExecutorPoolService;
import sprout.server.builtins.VirtualRequestExecutorService;

import java.util.List;
import java.util.concurrent.ExecutorService;

@Configuration
public class ServerConfiguration {

    @Bean
    public RequestExecutorService executorService(AppConfig appConfig, List<ContextPropagator> contextPropagators) {
        String threadType = appConfig.getStringProperty("server.thread-type", "virtual");
        if (threadType.equals("virtual")) {
            return new VirtualRequestExecutorService(contextPropagators);
        }
        return new RequestExecutorPoolService(appConfig.getIntProperty("server.thread-pool-size", 100));
    }

    @Bean
    public AcceptableProtocolHandler httpProtocolHandler(AppConfig appConfig, RequestDispatcher requestDispatcher, HttpRequestParser httpRequestParser, RequestExecutorService executorService) {
        String executionMode = appConfig.getStringProperty("server.execution-mode", "hybrid");
        if (executionMode.equals("hybrid")) {
            System.out.println("Execution mode is hybrid");
            return new BioHttpProtocolHandler(requestDispatcher, httpRequestParser, executorService);
        }
        System.out.println("Execution mode is NIO");
        return new NioHttpProtocolHandler(requestDispatcher, httpRequestParser, executorService);
    }
}
