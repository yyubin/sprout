# 🌱 **Sprout**

[English](#english) | **한국어** | [📖 상세 문서](https://yyubin.github.io/sprout/)

*Spring이 내부적으로 어떻게 작동하는지 이해하기 위해 처음부터 구축한 경량 Java 웹 프레임워크*
이제 **완전히 작동하는 NIO 및 하이브리드 서버**와 **비동기 WebSocket 스택**을 제공합니다. 여전히 **명확성 · 해킹 가능성 · 확장성**에 중점을 두고 있습니다.

**범위:** 컨테이너/AOP/웹/서버 내부에 집중. 완전한 ORM은 v1.0에서 의도적으로 제외하여 표면적을 작게 유지하고 코드를 쉽게 감사할 수 있도록 했습니다.

---

## ✨ 핵심 기능 (v1.0.0)

| 영역                                                                | 상태 및 주요 사항                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| ------------------------------------------------------------------- |--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **IoC / DI 컨테이너**                                              | • `@Component`, `@Service`, `@Controller`, `@Repository`, `@Configuration`, `@Aspect` 스캔<br/>• 생성자 기반 주입<br/>• `@Order` 지원을 통한 자동 `List<T>` 채우기<br/>• 순환 의존성 감지 (위상 정렬)<br/>• 합리적인 기본값을 위한 **`BeanDefinitionRegistrar`를 통한 자동 구성**                                                                                                                                                                                                    |
| **빈 정의**                                                | • `ConstructorBeanDefinition` & `MethodBeanDefinition`<br/>• 팩토리 메서드 또는 생성자 전략<br/>• **생성자 메타 캐시** → 필수 인수 생성자를 위한 안전한 프록시                                                                                                                                                                                                                                                                                                                                   |
| **AOP**                                             | • 어노테이션 기반 (`@Before`, `@After`, `@Around`)<br/>• AspectJ 스타일 포인트컷 (`*`, `..`, `?`)<br/>• Spring에서 영감을 받은 Advisor/Advice/Pointcut 계층<br/>• CGLIB 서브클래싱 + Objenesis 폴백 (기본 생성자 불필요)<br/>• 순서화된 어드바이저 체인, 대상별 프록시                                                                                                                                                                                                                   |
| **구성 프록시**                                             | `@Configuration(proxyBeanMethods = true)`를 위한 CGLIB 프록시 → 반복되는 `@Bean` 호출 캐시                                                                                                                                                                                                                                                                                                                                                                                                 |
| **웹 계층 (HTTP)**                                                | • 선언적 라우팅 (`@GetMapping`, `@PostMapping`, … + `{var}` 패턴)<br/>• `@PathVariable`, `@RequestParam`, `@RequestBody`, … 를 위한 ArgumentResolver<br/>• `RequestDispatcher` 바인드 → 호출 → 해결 (`ResponseEntity`, DTO, `void`)                                                                                                                                                                                                         |
| **서버**                                                          | **새로운 기능:** `java.nio.channels`를 기반으로 구축된 **NIO 서버**<br/>• **하이브리드 모드**: 가상 스레드 또는 클래식 풀을 통한 HTTP, NIO를 통한 WS; 구성에 따라 선택<br/>• 학습/단순함을 위한 블로킹 폴백 유지                                                                                                                                                                                                                                                          |
| **필터 및 인터셉터**                                          | • 서블릿 스타일 `Filter` 체인<br/>• 글로벌 필터 (인증, CORS, 로깅…)<br/>• 미들웨어 같은 `Interceptor` 체인<br/>• `RequestDispatcher`에 `List<Filter>` / `List<Interceptor>` 자동 주입                                                                                                                                                                                                                                                                                                   |
| **보안**                                                        | • 모듈식 인증 (`AuthenticationManager`, `AuthenticationProvider`, `UserDetailsService`)<br/>• `AuthenticationFilter`를 통한 사용자명/비밀번호 로그인<br/>• `@PreAuthorize`를 통한 메서드 보안 (AOP 기반)<br/>• `AuthorizationFilter`를 통한 URL 권한 부여<br/>• 요청별 `ThreadLocal`을 가진 `SecurityContextHolder`<br/>• 자동 구성 (`@EnableSproutSecurity`)                                                                                                                  |
| **예외 처리**                                              | • HTTP 예외 (`BadRequest`, `MethodNotAllowed`, …)<br/>• `@ControllerAdvice` + `@ExceptionHandler`<br/>• 확장 가능한 `ExceptionResolver` 체인                                                                                                                                                                                                                                                                                                                                |
| **데이터 접근**                                                     | • 경량 `JdbcTemplate` 추상화<br/>• HikariCP 통합<br/>• AOP 기반 `@Transactional` 지원<br/>• `TransactionManager` 추상화 (자동 커밋/롤백)                                                                                                                                                                                                                                                                                                                            |
| **WebSocket (비동기/NIO)**                                           | • RFC6455 핸드셰이크 + 프레임 파서/인코더 (마스킹, ping/pong, close)<br/>• **논블로킹 쓰기 큐**, OP_WRITE 토글링, 드레인 후 우아한 종료<br/>• 분할 처리 (텍스트/바이너리 연속 프레임)<br/>• `WebSocketSession` 추상화 + 생명주기 훅 (`@OnOpen`, `@OnMessage`, `@OnClose`, `@OnError`)<br/>• 플러그인 가능한 `WebSocketMessageDispatcher` & `WebSocketArgumentResolver`<br/>• HTTP NIO와 동일한 선택자 루프에서 실행하거나 별도로 실행—구성 가능 |
| **부트스트랩**                                                       | 한 줄: `SproutApplication.run()`이 컨테이너를 부팅하고 서버를 시작                                                                                                                                                                                                                                                                                                                                                                                                          |

---

## 🏃‍♂️ 빠른 시작

1. **클론 및 빌드**

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

*서버 모드/스레드 모델은 이제 CLI 인수가 아닌 `application.yml`에서 읽습니다.*

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
            System.out.printf("%s took %d µs%n", pjp.getSignature().toLongName(),
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
  thread-type: virtual     # virtual | platform (hybrid/blocking HTTP 워커에만 해당)
  thread-pool-size: 150    # thread-type = platform일 때 사용

sprout:
  database:
    url: jdbc:mysql://localhost:3306/sprout
    username: root
    password: change-me
```

> `AppConfig`는 컴포넌트로 등록 및 사용 가능하며, 주입받아 사용 가능합니다. 

### 서버 모드

| 모드      | HTTP                    | WebSocket | 사용 사례                          |
|-----------|-------------------------|-----------|-----------------------------------|
| blocking  | 플랫폼 스레드           | 지원 없음       | 디버깅/학습                |
| nio       | NIO 선택자              | NIO       | 높은 동시성/적은 메모리       |
| hybrid    | 가상/풀 (HTTP)         | NIO (WS)  | 프로덕션 학습 경험    |

### 스레드 유형

| 유형              | 사용 시기                                                    |
|-------------------|----------------------------------------------------------------|
| **virtual**       | 요청 핸들러의 기본 선택 (권장)             |
| **platform pool** | 동시성을 제한해야 할 때만 (JDBC 드라이버, 블로킹 I/O) |

### 연결 방식

* `AppConfig`가 YAML을 한 번 읽고 헬퍼를 제공: `getStringProperty`, `getIntProperty`.
* `ServerAutoConfigurationRegistrar`가 `server.*` 키를 검사하고 등록:

    * **HTTP 핸들러**: `NioHttpProtocolHandler` 또는 `BlockingHttpProtocolHandler` (hybrid/blocking용)
    * **RequestExecutorService**: `VirtualRequestExecutorService` (가상 스레드) 또는 `RequestExecutorPoolService` (고정 풀)

**팁:** 요청 핸들러에는 가상 스레드를 선호하세요; 동시성을 제한해야 할 때만 고정 풀로 전환하세요 (예: JDBC 드라이버나 블로킹 I/O).

---

## 🔌 WebSocket 예제 (NIO)

```java
@WebSocketEndpoint("/ws/chat")
public class ChatSocket {

    @OnOpen
    public void onOpen(WebSocketSession session) {
        System.out.println("WebSocket opened: " + session.getId());
    }

    @MessageMapping("/chat.send")
    public void handleMessage(WebSocketSession session, @RequestBody ChatMessage msg) {
        System.out.println("Message from " + session.getId() + ": " + msg.getText());
    }

    @OnClose
    public void onClose(WebSocketSession session) {
        System.out.println("WebSocket closed: " + session.getId());
    }
}
```

**`@WebSocketEndpoint` 및 `@MessageMapping`과 같은 어노테이션은 Sprout에서 제공됩니다 (JSR-356이 아님).**

---

## 🧪 테스트

**리포트:** [테스트](https://yyubin.github.io/sprout/tests/) ·
[커버리지](https://yyubin.github.io/sprout/coverage/)

**687개 테스트, 0개 실패 (100% 통과, Gradle 8.10.1 · 2025‑10‑21)**

**테스트 커버리지 (Jacoco):**
- **라인 커버리지: 85%**
- **브랜치 커버리지: 75%**

커버리지 하이라이트:

* **핵심 컨테이너**: 스캐닝, 빈 그래프/위상 정렬, 생성자 주입, `@Order` 리스트 주입
* **AOP**: 어드바이스 빌더/인터셉터, 어드바이저 레지스트리, 포인트컷 파싱
* **MVC 계층**: 요청 파싱 (라인/헤더/쿼리), 핸들러 매핑 및 호출, 인수 해결자, 예외 어드바이스
* **보안**: 인증 제공자, 비밀번호 인코딩, 컨텍스트 전파, 필터 및 권한 부여 애스펙트
* **서버 스택**: HTTP 블로킹/NIO/하이브리드 전략, 실행자 서비스 (가상 vs 풀), 프로토콜 감지기/핸들러
* **WebSocket**: 핸드셰이크, 프레임 인코더/파서, ping/pong, 분할, 비동기 쓰기 및 우아한 종료, 디스패처/해결자
* **유틸리티**: `HttpUtils` (Content-Length 및 청크), 응답 버퍼 생성, 기타 헬퍼

도구 및 스타일:

* JUnit 5 + Mockito (JDK-final 클래스를 위한 inline/lenient)
* 무거운 모킹보다 결정적 동작을 위한 가짜 구현 (예: 프레임 인코더/파서)
* 실제 소켓 없이 NIO 동작을 검증하기 위한 선택자/`interestOps` 상태 가짜
* 빌드 리포트: `build/reports/tests/test/index.html`

>  NIO 서버를 시작하고 실제 HTTP/WebSocket 클라이언트로 테스트하는 블랙박스 통합 테스트를 추가해보세요.

## 🗺️ 로드맵

| 릴리스  | 계획 / 완료                             | 비고                                           |
| -------- | ------------------------------------------ | ----------------------------------------------- |
| **v0.2** | ✅ AOP 핵심 제공                       | `@Before`/`@After`/`@Around`, AspectJ 포인트컷 |
| **v0.3** | ✅ 미들웨어 및 글로벌 인터셉터         | 필터 + 인터셉터 체인                    |
| **v0.4** | ✅ 데이터 접근 및 보안 핵심              | `JdbcTemplate`, `@Transactional`, 인증/권한   |
| **v0.5** | ✅ **NIO 및 하이브리드 서버**, 비동기 WebSocket | 선택자 루프, OP_WRITE 관리, 우아한 종료   |
| **v1.0** | 🎯 **안정된 API 및 프레임워크 성숙도**     | 프로덕션 사용 준비, 포괄적 문서    |

**v1.0 이후 로드맵:**

| 기능  | 상태 | 설명 |
| -------- | ------ | ----------- |
| **경량 ORM** | 🔄 계획됨 | 엔티티 매핑, 어노테이션 기반 ORM, 간단한 쿼리 DSL |
| **프로덕션 도구** | 🔄 계획됨 | 메트릭, 모니터링, 향상된 성능 프로파일링 |
| **고급 기능** | 🔄 계획됨 | 향상된 보안, 캐싱 레이어, 검증 프레임워크 |

> 로드맵은 목표 지향적입니다. 학습용 프로젝트로서, 우선순위 변동 가능성이 있습니다.

---

## 🙏 감사의 말

* **Spring Framework** — 감사합니다...🙇‍♀️
* **Reflections**, **CGLIB**, **Objenesis**, **Jackson** — 런타임 메타 프로그래밍 및 직렬화 백본.

---

## 🤝 기여

PR 및 이슈를 환영합니다. 로드맵 항목을 선택하거나 기능을 제안해보세요. 

---

## 📜 라이센스

MIT 라이센스. [`LICENSE`](LICENSE)를 참조하세요.
