package sprout.boot;

import sprout.context.Container;
import sprout.server.HttpServer;

public final class SproutApplication {

    public static void run(String yamlPath) throws Exception {
        // 1) 패키지 목록 로드
        var packages = YamlConfigLoader.loadPackages(yamlPath);

        // 2) DI 컨테이너 부트스트랩
        Container ctx = Container.getInstance();
        for (String p : packages) ctx.bootstrap(p.trim());

        // 3) 서버 구동
        HttpServer server = ctx.get(HttpServer.class);
        server.start(8080);                 // 포트만 넘기면 됨
    }
}