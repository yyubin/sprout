# 🌱 **Sprout**

[English](https://github.com/yyubin/sprout/blob/main/README.md) | **한국어** | [📖 Documentation](https://yyubin.github.io/sprout/)

*Spring이 내부적으로 어떻게 작동하는지 이해하기 위해 처음부터 만든 경량 Java 웹 프레임워크.*
이제 **완전히 작동하는 NIO & 하이브리드 서버**와 **비동기 WebSocket 스택**을 제공합니다. 여전히 **명확성 · 해킹 가능성 · 확장성**을 중시합니다.

**범위:** 컨테이너/AOP/웹/서버 내부 구조에 집중합니다. 전체 ORM은 v1.0에서 의도적으로 제외하여 범위를 작게 유지하고 코드 감사를 쉽게 만들었습니다.

---

## ✨ 핵심 기능 (v1.0.0)

| 영역                                                                | 상태 & 주요 특징                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| ------------------------------------------------------------------- |--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **IoC / DI 컨테이너**                                              | • `@Component`, `@Service`, `@Controller`, `@Repository`, `@Configuration`, `@Aspect` 스캔.<br/>  • 생성자 기반 주입.<br/>• `@Order` 지원과 함께 자동 `List<T>` 채우기.<br/>• 순환 의존성 감지 (위상 정렬).<br/>• **`BeanDefinitionRegistrar`를 통한 자동 구성** (합리적인 기본값 제공).                                                                                                                                                      |
| **빈 정의**                                                | • `ConstructorBeanDefinition` & `MethodBeanDefinition`.<br/>• 팩토리 메서드 또는 생성자 전략.<br/>• **생성자 메타 캐시** → 필수 인자 생성자를 위한 안전한 프록시 생성.                                                                                                                                                                                                                   |
| **AOP**                                                             | • 어노테이션 기반 (`@Before`, `@After`, `@Around`).<br/>• AspectJ 스타일 포인트컷 (`*`, `..`, `?`).<br/>• Spring에서 영감을 받은 Advisor/Advice/Pointcut 계층 구조.<br/>• CGLIB 서브클래싱 + Objenesis 폴백 (기본 생성자 불필요).<br/>• 순서가 지정된 advisor 체인, 타겟별 프록시.                                                                                                                                   |
| **구성 프록시**                                             | `@Configuration(proxyBeanMethods = true)`를 위한 CGLIB 프록시 → 반복되는 `@Bean` 호출 캐싱.                                                                                                                                                                                                                                                                                 |
| **웹 레이어 (HTTP)**                                                | • 선언적 라우팅 (`@GetMapping`, `@PostMapping`, … + `{var}` 패턴).<br/>• `@PathVariable`, `@RequestParam`, `@RequestBody` 등을 위한 ArgumentResolver.<br/>• `RequestDispatcher`가 바인딩 → 호출 → 해결 (`ResponseEntity`, DTO, `void`).                                                                                                                                         |
| **서버**                                                          | **신규:** `java.nio.channels` 기반 **NIO 서버**.<br/>• **하이브리드 모드**: 가상 스레드 또는 클래식 풀을 통한 HTTP, NIO를 통한 WS; 설정에 따라 선택 가능.<br/>• 학습/단순성을 위한 블로킹 폴백 유지.                                                                                                                                                                                          |
| **필터 & 인터셉터**                                          | • 서블릿 스타일 `Filter` 체인.<br/>• 글로벌 필터 (인증, CORS, 로깅…).<br/>• 미들웨어 스타일 `Interceptor` 체인.<br/>• `RequestDispatcher`에 `List<Filter>` / `List<Interceptor>` 자동 주입.                                                                                                                                                                                                   |
| **보안**                                                        | • 모듈식 인증 (`AuthenticationManager`, `AuthenticationProvider`, `UserDetailsService`).<br/>• `AuthenticationFilter`를 통한 사용자명/비밀번호 로그인.<br/>• `@PreAuthorize`를 통한 메서드 보안 (AOP 기반).<br/>• `AuthorizationFilter`를 통한 URL 권한 부여.<br/>• 요청별 `ThreadLocal`을 사용하는 `SecurityContextHolder`.<br/>• 자동 구성 (`@EnableSproutSecurity`).                                                                                                                  |
| **예외 처리**                                              | • HTTP 예외 (`BadRequest`, `MethodNotAllowed`, …).<br/>• `@ControllerAdvice` + `@ExceptionHandler`.<br/>• 확장 가능한 `ExceptionResolver` 체인.                                                                                                 |
| **데이터 액세스**                                                     | • 경량 `JdbcTemplate` 추상화.<br/>• HikariCP 통합.<br/>• AOP 기반 `@Transactional` 지원.<br/>• `TransactionManager` 추상화 (자동 커밋/롤백).                                                                                                                                                                                                                                                            |
| **WebSocket (비동기/NIO)**                                           | • RFC6455 핸드셰이크 + 프레임 파서/인코더 (마스킹, ping/pong, close).<br/>• **논블로킹 쓰기 큐**, OP\_WRITE 토글, 드레인 후 우아한 종료.<br/>• 단편화 처리 (텍스트/바이너리 연속 프레임).<br/>• `WebSocketSession` 추상화 + 생명주기 훅 (`@OnOpen`, `@OnMessage`, `@OnClose`, `@OnError`).<br/>• 플러그형 `WebSocketMessageDispatcher` & `WebSocketArgumentResolver`.<br/>• HTTP NIO와 동일한 셀렉터 루프에서 실행되거나 별도로 실행 가능—구성 가능. |
| **부트스트랩**                                                       | 한 줄로 실행: `SproutApplication.run()`이 컨테이너를 부팅하고 서버를 시작합니다.                                                                                                                                                                                                                                                                                                                                                                                                          |

---

## 🏃‍♂️ 빠른 시작

1. **클론 & 빌드**

```bash
$ git clone https://github.com/yyubin/sprout.git
$ cd sprout && ./gradlew build
```

2. **실행**

> Java 21 + CGLIB 프록시는 `--add-opens` 플래그가 필요합니다 (깊은 리플렉션을 제거할 때까지):

```bash
$ java \
  --add-opens=java.base/java.lang=ALL-UNNAMED \
  --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
  --add-opens=java.base/java.io=ALL-UNNAMED \
  --add-opens=java.base/java.util=ALL-UNNAMED \
  -jar build/libs/sprout.jar
```

*서버 모드/스레드 모델은 이제 CLI 인자가 아닌 `application.yml`에서 읽어옵니다.*

3. **최소 예제** (변경 없음)

```java
@ComponentScan("app")
public class DemoApplication {
    public static void main(String[] args) throws Exception {
        SproutApplication.run(DemoApplication.class);
    }
}
```

```java
@Aspect
public class LoggingAspect {
    @Around(pointcut = "app..*Service.*")
    public Object logExecTime(ProceedingJoinPoint pjp) throws Throwable {
        long t0 = System.nanoTime();
        try {
            return pjp.proceed();
        } finally {
            System.out.printf("%s took %d µs%n", pjp.getSignature().toLongString(),
                              (System.nanoTime()-t0)/1_000);
        }
    }
}
```

```java
@Controller
@RequestMapping("/api")
public class TestController {
    private final GreetingService svc;
    public TestController(GreetingService svc) { this.svc = svc; }

    @GetMapping("/hello/{id}")
    public MessageDto hello(@PathVariable Long id) {
        return new MessageDto(svc.greet(id));
    }
}
```

---

## ⚙️ 구성 (`application.yml`)

Sprout는 시작 시 `AppConfig`를 통해 `application.yml`을 로드합니다. 중첩된 키는 점 표기법으로 해결됩니다 (예: `server.execution-mode`).

```yaml
author: you
server:
  execution-mode: hybrid   # nio | hybrid (기본값: hybrid)
  thread-type: virtual     # virtual | platform (하이브리드/블로킹 HTTP 워커에만 적용)
  thread-pool-size: 150    # thread-type = platform일 때 사용

sprout:
  database:
    url: jdbc:mysql://localhost:3306/sprout
    username: root
    password: change-me
```

### 서버 모드

| 모드      | HTTP                    | WebSocket | 사용 사례                          |
|-----------|-------------------------|-----------|-----------------------------------|
| blocking  | 플랫폼 스레드           | 해당 없음 | 디버깅/학습                        |
| nio       | NIO 셀렉터              | NIO       | 높은 동시성/낮은 메모리            |
| hybrid    | 가상/풀 (HTTP)          | NIO (WS)  | 프로덕션 학습 경험                 |

### 스레드 유형

| 유형              | 사용 시기                                                    |
|-------------------|----------------------------------------------------------------|
| **virtual**       | 요청 핸들러의 기본 선택 (권장)                                |
| **platform pool** | 동시성을 제한해야 할 때만 (JDBC 드라이버, 블로킹 I/O)        |

### 연결 방식

* `AppConfig`가 YAML을 한 번 읽고 헬퍼를 제공: `getStringProperty`, `getIntProperty`.
* `ServerAutoConfigurationRegistrar`가 `server.*` 키를 검사하고 등록:

  * **HTTP 핸들러**: `NioHttpProtocolHandler` 또는 `BlockingHttpProtocolHandler` (하이브리드/블로킹용)
  * **RequestExecutorService**: `VirtualRequestExecutorService` (가상 스레드) 또는 `RequestExecutorPoolService` (고정 풀)

**팁:** 요청 핸들러에는 가상 스레드를 선호하세요; 동시성을 제한해야 할 때만 고정 풀로 전환하세요 (예: JDBC 드라이버 또는 블로킹 I/O).

---

## 🎥 WebSocket 데모 & 벤치마크

[![Sprout WebSocket Demo](https://img.youtube.com/vi/7ypz7RCcZps/0.jpg)](https://www.youtube.com/watch?v=7ypz7RCcZps)

이 비디오는 Sprout의 **완전한 논블로킹 NIO WebSocket 스택**이 작동하는 모습을 보여줍니다.

**데모에서 보여지는 것:**
- NIO 기반 WebSocket 핸드셰이크 & 프레임 처리
- 동시 클라이언트 연결
- Echo, 브로드캐스트, 채팅 스타일 메시징
- Ping/Pong 프레임
- 우아한 종료 처리
- OP_WRITE 토글을 사용한 논블로킹 쓰기 큐

비디오에서 사용된 핸들러는 Sprout의 API로 구축된 실제 애플리케이션 레벨 WebSocket 핸들러입니다 (아래 참조).

---

## 🧩 WebSocket 예제
```java
@Component
@WebSocketHandler("/ws/benchmark")
public class WebSocketBenchmarkHandler {

    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }

    @OnClose
    public void onClose(WebSocketSession session, CloseCode code) {
        sessions.remove(session.getId());
    }

    @MessageMapping("/echo")
    public void echo(WebSocketSession session, @Payload String msg) throws IOException {
        session.sendText(msg);
    }

    @MessageMapping("/broadcast")
    public void broadcast(WebSocketSession session, @Payload String msg) throws IOException {
        for (WebSocketSession s : sessions.values()) {
            if (s.isOpen()) s.sendText(msg);
        }
    }

    @MessageMapping("/ping")
    public void ping(WebSocketSession session, @Payload String ignored) throws IOException {
        session.sendPing("ping".getBytes());
    }
}
```
> 논블로킹 쓰기 및 프레임 레벨 제어와 함께 Sprout의 NIO 셀렉터 루프에서 실행됩니다.

---

## 🧪 테스트

**리포트:** [Tests](https://yyubin.github.io/sprout/tests/) ·
[Coverage](https://yyubin.github.io/sprout/coverage/)

**687개 테스트, 0개 실패 (100% 통과, Gradle 8.10.1 · 2025‑10-27)**

**테스트 커버리지 (Jacoco):**
- **라인 커버리지: 85%**
- **브랜치 커버리지: 75%**

커버리지 하이라이트:

* **핵심 컨테이너**: 스캐닝, 빈 그래프/위상 정렬, 생성자 주입, `@Order` 리스트 주입
* **AOP**: advice 빌더/인터셉터, advisor 레지스트리, 포인트컷 파싱
* **MVC 레이어**: 요청 파싱 (라인/헤더/쿼리), 핸들러 매핑 & 호출, argument resolver, 예외 advice
* **보안**: 인증 provider, 비밀번호 인코딩, 컨텍스트 전파, 필터 & 권한 부여 aspect
* **서버 스택**: HTTP Blocking/NIO/Hybrid 전략, executor service (가상 vs 풀), 프로토콜 detector/handler
* **WebSocket**: 핸드셰이크, 프레임 인코더/파서, ping/pong, 단편화, 비동기 쓰기 & 우아한 종료, dispatcher/resolver
* **유틸리티**: `HttpUtils` (Content-Length & chunked), 응답 버퍼 생성, 기타 헬퍼

도구 & 스타일:

* JUnit 5 + Mockito (JDK-final 클래스를 위한 인라인/lenient)
* 무거운 모킹보다 결정론적 동작을 선호하는 가짜 구현 (예: 프레임 인코더/파서)
* 실제 소켓 없이 NIO 동작을 검증하기 위한 Selector/`interestOps` 상태 가짜
* 빌드 리포트: `build/reports/tests/test/index.html`

--- 

## 🗺️ 로드맵

| 릴리스  | 계획 / 완료                             | 비고                                           |
| -------- | ------------------------------------------ | ----------------------------------------------- |
| **v0.2** | ✅ AOP 핵심 제공                           | `@Before`/`@After`/`@Around`, AspectJ 포인트컷 |
| **v0.3** | ✅ 미들웨어 & 글로벌 인터셉터               | 필터 + 인터셉터 체인                            |
| **v0.4** | ✅ 데이터 액세스 & 보안 핵심                | `JdbcTemplate`, `@Transactional`, 인증/권한부여 |
| **v0.5** | ✅ **NIO & 하이브리드 서버**, 비동기 WebSocket | 셀렉터 루프, OP\_WRITE 관리, 우아한 종료       |
| **v1.0** | 🎯 **안정적인 API & 프레임워크 성숙도**    | 프로덕션 사용 준비, 포괄적인 문서               |

**v1.0 이후 로드맵:**

| 기능  | 상태 | 설명 |
| -------- | ------ | ----------- |
| **경량 ORM** | 🔄 계획됨 | 엔티티 매핑, 어노테이션 기반 ORM, 간단한 쿼리 DSL |
| **프로덕션 도구** | 🔄 계획됨 | 메트릭, 모니터링, 향상된 성능 프로파일링 |
| **고급 기능** | 🔄 계획됨 | 향상된 보안, 캐싱 레이어, 검증 프레임워크 |

> 로드맵은 지향점 입니다. 학습 계획에 따라 변동 가능성이 있습니다.

---

## 🙏 감사의 말

* **Spring Framework** — 감사합니다..  
* **Reflections**, **CGLIB**, **Objenesis**, **Jackson** — 런타임 메타 프로그래밍 & 직렬화 백본.

---

## 🤝 기여

PR & 이슈를 환영합니다. 로드맵 항목을 선택하거나 기능을 제안해주세요.

---

## 📜 라이선스

MIT License. [`LICENSE`](LICENSE) 참조.