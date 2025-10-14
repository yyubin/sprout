package benchmark;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import java.time.Duration;

public class CpuIntensiveSimulation extends Simulation {

    HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://localhost:8080")
            .acceptHeader("text/plain,application/json")
            .userAgentHeader("Gatling Performance Test")
            .shareConnections()
            .disableFollowRedirect()
            .disableWarmUp()
            .maxConnectionsPerHost(200)
            .connectionHeader("keep-alive")
            .header("Keep-Alive", "timeout=10, max=1000");

    // 시나리오 1 : Moderate CPU load (fibonacci)
    ScenarioBuilder cpuScenario = scenario("CPU Load Test")
            .exec(
                    http("GET /benchmark/cpu")
                            .get("/benchmark/cpu?n=35")
                            .check(status().is(200))
            );

    // 시나리오 2⃣ : Heavy CPU load (prime calculation)
    ScenarioBuilder cpuHeavyScenario = scenario("Heavy CPU Load Test")
            .exec(
                    http("GET /benchmark/cpu-heavy")
                            .get("/benchmark/cpu-heavy?limit=10000")
                            .check(status().is(200))
            );

    {
        setUp(
                // Warm-up 및 중간 부하 구간
                cpuScenario.injectOpen(
                        rampUsers(5).during(Duration.ofSeconds(5)),   // warm-up
                        rampUsers(20).during(Duration.ofSeconds(10)), // gradual ramp
                        constantUsersPerSec(30).during(Duration.ofSeconds(20)), // sustained
                        rampUsersPerSec(30).to(50).during(Duration.ofSeconds(10)), // peak ramp
                        constantUsersPerSec(50).during(Duration.ofSeconds(15))     // steady peak
                ).protocols(httpProtocol),

                // Heavy workload 구간
                cpuHeavyScenario.injectOpen(
                        nothingFor(Duration.ofSeconds(10)), // warm-up 뒤 실행
                        rampUsers(10).during(Duration.ofSeconds(10)),
                        constantUsersPerSec(20).during(Duration.ofSeconds(20))
                ).protocols(httpProtocol)
        )
                .assertions(
                        global().responseTime().mean().lt(5000),         // 평균 응답 < 5s
                        global().responseTime().percentile(95.0).lt(10000), // 95% < 10s
                        global().successfulRequests().percent().gt(99.0) // 성공률 > 99%
                );
    }
}
