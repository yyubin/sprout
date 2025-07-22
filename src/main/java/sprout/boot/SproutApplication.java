package sprout.boot;

import sprout.beans.annotation.ComponentScan;
import sprout.config.AppConfig;
import sprout.context.ApplicationContext;
import sprout.context.builtins.SproutApplicationContext;
import sprout.server.HttpServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class SproutApplication {

    public static void run(Class<?> primarySource) throws Exception {
        List<String> packages = getPackagesToScan(primarySource);

        ApplicationContext applicationContext = new SproutApplicationContext(packages.toArray(new String[packages.size()]));
        applicationContext.refresh();

        HttpServer server = applicationContext.getBean(HttpServer.class);
        int port = applicationContext.getBean(AppConfig.class).getIntProperty("server.port", 8080);
        server.start(port);
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