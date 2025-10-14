package benchmark;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import java.time.Duration;

public class HelloWorldSimulation extends Simulation {

    HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://localhost:8080")
            .acceptHeader("text/plain,application/json")
            .userAgentHeader("Gatling Performance Test")
            .shareConnections() // 커넥션 재사용
            .disableFollowRedirect()
            .disableWarmUp() // Gatling 기본 warm-up 비활성화
            .maxConnectionsPerHost(200)
            .connectionHeader("keep-alive")
            .header("Keep-Alive", "timeout=5, max=1000");

    // Warm-up 시나리오
    // 목적: 서버 및 JVM JIT, 스레드풀, 소켓 풀 예열
    ScenarioBuilder warmUp = scenario("Warm-up Phase")
            .exec(
                    http("Warm-up request")
                            .get("/benchmark/hello")
                            .check(status().is(200))
            );

    // 본격 부하 시나리오
    ScenarioBuilder loadTest = scenario("Hello World Load Test")
            .exec(
                    http("GET /benchmark/hello")
                            .get("/benchmark/hello")
                            .check(status().is(200))
            );

    {
        setUp(
                // Warm-up 단계: 낮은 부하로 10초간 서버를 예열
                warmUp.injectOpen(
                        rampUsers(5).during(Duration.ofSeconds(5)),
                        constantUsersPerSec(10).during(Duration.ofSeconds(10))
                ).protocols(httpProtocol),

                // 본 테스트 단계: warm-up 후 바로 실행
                loadTest.injectOpen(
                        nothingFor(Duration.ofSeconds(15)), // warm-up 이후 실행
                        rampUsers(10).during(Duration.ofSeconds(5)),
                        rampUsers(50).during(Duration.ofSeconds(10)),
                        constantUsersPerSec(100).during(Duration.ofSeconds(30)),
                        rampUsersPerSec(100).to(200).during(Duration.ofSeconds(10)),
                        constantUsersPerSec(200).during(Duration.ofSeconds(20))
                ).protocols(httpProtocol)
        )
                .assertions(
                        global().responseTime().max().lt(1000),
                        global().successfulRequests().percent().gt(99.0)
                );
    }
}
