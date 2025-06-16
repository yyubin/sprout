package sprout.boot;

import sprout.beans.annotation.ComponentScan;
import sprout.context.Container;
import sprout.server.HttpServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class SproutApplication {

    public static void run(Class<?> primarySource) throws Exception { // Class<?> 를 인자로 받도록 변경
        // 1) 패키지 목록 로드 (ComponentScan 어노테이션에서 추출)
        List<String> packages = getPackagesToScan(primarySource);

        // 2) DI 컨테이너 부트스트랩
        Container ctx = Container.getInstance();
        for (String p : packages) ctx.bootstrap(p.trim());

        // 3) 서버 구동
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