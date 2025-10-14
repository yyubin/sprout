package benchmark;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import java.time.Duration;

public class LatencySimulation extends Simulation {

    HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://localhost:8080")
            .acceptHeader("text/plain,application/json")
            .userAgentHeader("Gatling Performance Test")
            .shareConnections()
            .disableFollowRedirect()
            .disableWarmUp()
            .maxConnectionsPerHost(300)
            .connectionHeader("keep-alive")
            .header("Keep-Alive", "timeout=10, max=1000");


    // 시나리오 1: Low latency (50ms)
    ScenarioBuilder lowLatencyScenario = scenario("Low Latency Test")
            .exec(
                    http("GET /benchmark/latency?ms=50")
                            .get("/benchmark/latency?ms=50")
                            .check(status().is(200))
            );

    // 시나리오 2: Medium latency (100ms)
    ScenarioBuilder mediumLatencyScenario = scenario("Medium Latency Test")
            .exec(
                    http("GET /benchmark/latency?ms=100")
                            .get("/benchmark/latency?ms=100")
                            .check(status().is(200))
            );

    // 시나리오 3: High latency (200ms)
    ScenarioBuilder highLatencyScenario = scenario("High Latency Test")
            .exec(
                    http("GET /benchmark/latency?ms=200")
                            .get("/benchmark/latency?ms=200")
                            .check(status().is(200))
            );

    // 시나리오 4: Mixed workload (CPU + I/O)
    ScenarioBuilder mixedScenario = scenario("Mixed Workload Test")
            .exec(
                    http("GET /benchmark/mixed")
                            .get("/benchmark/mixed")
                            .check(status().is(200))
            );

    {
        setUp(
                // Low latency phase
                lowLatencyScenario.injectOpen(
                        rampUsers(20).during(Duration.ofSeconds(5)),
                        constantUsersPerSec(100).during(Duration.ofSeconds(20)),
                        rampUsersPerSec(100).to(300).during(Duration.ofSeconds(10)),
                        constantUsersPerSec(300).during(Duration.ofSeconds(20))
                ).protocols(httpProtocol),

                // Medium latency phase
                mediumLatencyScenario.injectOpen(
                        nothingFor(Duration.ofSeconds(5)),
                        rampUsers(20).during(Duration.ofSeconds(5)),
                        constantUsersPerSec(80).during(Duration.ofSeconds(20)),
                        rampUsersPerSec(80).to(200).during(Duration.ofSeconds(10)),
                        constantUsersPerSec(200).during(Duration.ofSeconds(20))
                ).protocols(httpProtocol),

                // High latency phase
                highLatencyScenario.injectOpen(
                        nothingFor(Duration.ofSeconds(10)),
                        rampUsers(10).during(Duration.ofSeconds(5)),
                        constantUsersPerSec(50).during(Duration.ofSeconds(20)),
                        rampUsersPerSec(50).to(150).during(Duration.ofSeconds(10)),
                        constantUsersPerSec(150).during(Duration.ofSeconds(15))
                ).protocols(httpProtocol),

                // Mixed workload
                mixedScenario.injectOpen(
                        nothingFor(Duration.ofSeconds(15)),
                        rampUsers(30).during(Duration.ofSeconds(10)),
                        constantUsersPerSec(100).during(Duration.ofSeconds(25))
                ).protocols(httpProtocol)
        )
                .assertions(
                        global().responseTime().mean().lt(1000), // 평균 응답 < 1초
                        global().responseTime().percentile(99.0).lt(3000), // P99 < 3초
                        global().successfulRequests().percent().gt(99.0)   // 성공률 > 99%
                );
    }
}
