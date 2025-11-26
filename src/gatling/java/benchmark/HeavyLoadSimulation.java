package benchmark;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import java.time.Duration;

/**
 * Heavy Load Simulation for JIT Profiling
 * - Warm-up: ~20,000 requests
 * - Load test: ~80,000 requests
 * - Total: ~100,000 requests
 */
public class HeavyLoadSimulation extends Simulation {

    HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://localhost:8080")
            .acceptHeader("text/plain,application/json")
            .userAgentHeader("Gatling Heavy Load Test")
            .shareConnections() // 커넥션 재사용
            .disableFollowRedirect()
            .disableWarmUp() // Gatling 기본 warm-up 비활성화
            .maxConnectionsPerHost(500)
            .connectionHeader("keep-alive")
            .header("Keep-Alive", "timeout=5, max=1000");

    // Warm-up 시나리오: 약 20,000 요청
    // 목적: 서버 JVM JIT 컴파일, 스레드풀, 커넥션 풀 예열
    ScenarioBuilder warmUp = scenario("Warm-up Phase")
            .exec(
                    http("Warm-up request")
                            .get("/benchmark/hello")
                            .check(status().is(200))
            );

    // Heavy Load 시나리오: 약 80,000 요청
    // 목적: JIT 컴파일 완료 후 steady-state 성능 측정
    ScenarioBuilder heavyLoad = scenario("Heavy Load Test")
            .exec(
                    http("GET /benchmark/hello")
                            .get("/benchmark/hello")
                            .check(status().is(200))
            );

    {
        setUp(
                // ========================================
                // Phase 1: Warm-up (~20,000 requests)
                // ========================================
                warmUp.injectOpen(
                        // 점진적 증가: 0 -> 100 RPS (5초)
                        rampUsersPerSec(0).to(100).during(Duration.ofSeconds(5)),
                        // 안정적 부하: 100 RPS (10초) = 1,000 requests
                        constantUsersPerSec(100).during(Duration.ofSeconds(10)),
                        // 중간 부하: 300 RPS (20초) = 6,000 requests
                        constantUsersPerSec(300).during(Duration.ofSeconds(20)),
                        // 고부하: 500 RPS (25초) = 12,500 requests
                        constantUsersPerSec(500).during(Duration.ofSeconds(25))
                        // Total: ~19,500 requests
                ).protocols(httpProtocol),

                // ========================================
                // Phase 2: Heavy Load (~80,000 requests)
                // ========================================
                heavyLoad.injectOpen(
                        // warm-up 완료 대기
                        nothingFor(Duration.ofSeconds(60)),

                        // 점진적 램프업: 100 -> 1000 RPS (10초)
                        rampUsersPerSec(100).to(1000).during(Duration.ofSeconds(10)),

                        // 고부하 구간 1: 1000 RPS (30초) = 30,000 requests
                        constantUsersPerSec(1000).during(Duration.ofSeconds(30)),

                        // 최대 부하: 1000 -> 1500 RPS (10초)
                        rampUsersPerSec(1000).to(1500).during(Duration.ofSeconds(10)),

                        // 고부하 구간 2: 1500 RPS (30초) = 45,000 requests
                        constantUsersPerSec(1500).during(Duration.ofSeconds(30)),

                        // 점진적 감소: 1500 -> 500 RPS (10초)
                        rampUsersPerSec(1500).to(500).during(Duration.ofSeconds(10))
                        // Total: ~80,000 requests
                ).protocols(httpProtocol)
        )
                .assertions(
                        // 최대 응답시간 1초 이하
                        global().responseTime().max().lt(1000),
                        // 95 percentile 응답시간 500ms 이하
                        global().responseTime().percentile3().lt(500),
                        // 성공률 99% 이상
                        global().successfulRequests().percent().gt(99.0),
                        // 초당 요청 처리량 (throughput) 체크
                        global().requestsPerSec().gt(500.0)
                );
    }
}
