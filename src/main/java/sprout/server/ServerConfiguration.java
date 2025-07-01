package sprout.server;

import sprout.beans.annotation.Bean;
import sprout.beans.annotation.Configuration;
import sprout.config.AppConfig;
import sprout.server.builtins.ThreadPoolService;
import sprout.server.builtins.VirtualThreadService;

@Configuration(proxyBeanMethods = true)
public class ServerConfiguration {

    @Bean
    public ThreadService threadService(AppConfig appConfig) {
        String threadType = appConfig.getStringProperty("server.thread-type", "virtual");

        if ("platform".equalsIgnoreCase(threadType)) {
            int poolSize = appConfig.getIntProperty("server.thread-pool-size", 10);
            System.out.println("Initializing with Platform Threads. Pool size: " + poolSize);
            return new ThreadPoolService(poolSize);
        }
        System.out.println("Initializing with Virtual Threads.");
        return new VirtualThreadService();
    }

}
