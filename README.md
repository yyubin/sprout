# 🌱 **Sprout**

*A lightweight Java web framework built from scratch to demystify how Spring works under the hood.*
Now with **fully working NIO & hybrid servers** and an **async WebSocket stack**. Still opinionated about **clarity · hackability · extensibility**.

**Scope:** Focused on container/AOP/web/server internals. A full ORM is intentionally out of scope for v1.0 to keep the surface area small and the code easy to audit.

---

## ✨ Core Features (v1.0.0)

| Area                                                                | Status & Highlights                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| ------------------------------------------------------------------- |--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **IoC / DI Container**                                              | • Scans `@Component`, `@Service`, `@Controller`, `@Repository`, `@Configuration`, `@Aspect`.<br/>  • Constructor‑based injection.<br/>• Automatic `List<T>` population with `@Order` support.<br/>• Cyclic‑dependency detection (topological sort).<br/>• **Auto‑configuration via `BeanDefinitionRegistrar`** for sensible defaults.                                                                                                                                                      |
| **Bean Definitions**                                                | • `ConstructorBeanDefinition` & `MethodBeanDefinition`.<br/>• Factory‑method or ctor strategy.<br/>• **Ctor‑meta cache** → safe proxying for required‑args constructors.                                                                                                                                                                                                                                                                                                                   |
| **AOP**                                                             | • Annotation‑driven (`@Before`, `@After`, `@Around`).<br/>• AspectJ‑style pointcuts (`*`, `..`, `?`).<br/>• Advisor/Advice/Pointcut hierarchy inspired by Spring.<br/>• CGLIB subclassing + Objenesis fallback (no no‑arg ctor required).<br/>• Ordered advisor chain, proxy‑per‑target.                                                                                                                                                                                                   |
| **Configuration Proxy**                                             | CGLIB proxy for `@Configuration(proxyBeanMethods = true)` → caches repeated `@Bean` calls.                                                                                                                                                                                                                                                                                                                                                                                                 |
| **Web Layer (HTTP)**                                                | • Declarative routing (`@GetMapping`, `@PostMapping`, … + `{var}` patterns).<br/>• ArgumentResolvers for `@PathVariable`, `@RequestParam`, `@RequestBody`, …<br/>• `RequestDispatcher` binds → invokes → resolves (`ResponseEntity`, DTO, `void`).                                                                                                                                                                                                                                         |
| **Server**                                                          | **NEW:** **NIO server** built on `java.nio.channels`.<br/>• **Hybrid mode**: HTTP over virtual threads or classic pool, WS over NIO; you choose per config.<br/>• Blocking fallback remains for learning / simplicity.                                                                                                                                                                                                                                                                          |
| **Filters & Interceptors**                                          | • Servlet‑style `Filter` chain.<br/>• Global filters (auth, CORS, logging…).<br/>• Middleware‑like `Interceptor` chain.<br/>• Auto‑inject `List<Filter>` / `List<Interceptor>` into `RequestDispatcher`.                                                                                                                                                                                                                                                                                   |
| **Security**                                                        | • Modular auth (`AuthenticationManager`, `AuthenticationProvider`, `UserDetailsService`).<br/>• Username/password login via `AuthenticationFilter`.<br/>• Method security with `@PreAuthorize` (AOP based).<br/>• URL authorization via `AuthorizationFilter`.<br/>• `SecurityContextHolder` with `ThreadLocal` per request.<br/>• Auto‑config (`@EnableSproutSecurity`).                                                                                                                  |
| **Exception Handling**                                              | • HTTP exceptions (`BadRequest`, `MethodNotAllowed`, …).<br/>• `@ControllerAdvice` + `@ExceptionHandler`.                                                                                                                                                                                                                                    <br/> • Extensible `ExceptionResolver` chain.                                                                                                 |                                                                                                                                                                                                                                                                                                                                                                                                                   |
| **Data Access**                                                     | • Lightweight `JdbcTemplate` abstraction.<br/>• HikariCP integration.<br/>• AOP‑driven `@Transactional` support.<br/>• `TransactionManager` abstraction (auto‑commit/rollback).                                                                                                                                                                                                                                                                                                            |
| **WebSocket (async/NIO)**                                           | • RFC6455 handshake + frame parser/encoder (masking, ping/pong, close).<br/>• **Non‑blocking write queue**, OP\_WRITE toggling, graceful close after drain.<br/>• Fragmentation handling (text/binary continuation frames).<br/>• `WebSocketSession` abstraction + lifecycle hooks (`@OnOpen`, `@OnMessage`, `@OnClose`, `@OnError`).<br/>• Pluggable `WebSocketMessageDispatcher` & `WebSocketArgumentResolver`.<br/>• Runs on same selector loop as HTTP NIO or separately—configurable. |                                                                                                                                                                                                                                                                                                                                                                                                                   |
| **Bootstrap**                                                       | One‑liner: `SproutApplication.run()` boots the container *and* starts the server.                                                                                                                                                                                                                                                                                                                                                                                                          |

---

## 🏃‍♂️ Quick Start

1. **Clone & build**

```bash
$ git clone https://github.com/yyubin/sprout.git
$ cd sprout && ./gradlew build
```

2. **Run**

> Java 21 + CGLIB proxies require `--add-opens` flags (until we drop deep reflection):

```bash
$ java \
  --add-opens=java.base/java.lang=ALL-UNNAMED \
  --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
  --add-opens=java.base/java.io=ALL-UNNAMED \
  --add-opens=java.base/java.util=ALL-UNNAMED \
  -jar build/libs/sprout.jar
```

*Server mode / thread model are now read from `application.yml`, not CLI args.*

3. **Minimal example** (unchanged)

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

## ⚙️ Configuration (`application.yml`)

Sprout loads `application.yml` at startup via `AppConfig`. Nested keys are resolved with dot notation (e.g. `server.execution-mode`).

```yaml
author: you
server:
  execution-mode: hybrid   # nio | hybrid (default: hybrid)
  thread-type: virtual     # virtual | platform (only for hybrid/blocking HTTP workers)
  thread-pool-size: 150    # used when thread-type = platform

sprout:
  database:
    url: jdbc:mysql://localhost:3306/sprout
    username: root
    password: change-me
```

### Server Modes

| Mode      | HTTP                    | WebSocket | Use Case                          |
|-----------|-------------------------|-----------|-----------------------------------|
| blocking  | platform threads       | n/a       | debugging/learning                |
| nio       | NIO selector           | NIO       | high concurrency/low memory       |
| hybrid    | virtual/pool (HTTP)    | NIO (WS)  | production learning experience    |

### Thread Types

| Type              | When to Use                                                    |
|-------------------|----------------------------------------------------------------|
| **virtual**       | Default choice for request handlers (recommended)             |
| **platform pool** | Only when you must bound concurrency (JDBC drivers, blocking I/O) |

### How it's wired

* `AppConfig` reads the YAML once and exposes helpers: `getStringProperty`, `getIntProperty`.
* `ServerAutoConfigurationRegistrar` inspects `server.*` keys and registers:

    * **HTTP handler**: `NioHttpProtocolHandler` or `BlockingHttpProtocolHandler` (for hybrid/blocking)
    * **RequestExecutorService**: `VirtualRequestExecutorService` (virtual threads) or `RequestExecutorPoolService` (fixed pool)

**Tip:** Prefer virtual threads for request handlers; switch to a fixed pool only when you must bound concurrency (e.g., JDBC drivers or blocking I/O).

---

## 🔌 WebSocket Example (NIO)

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

**Annotations such as `@WebSocketEndpoint` and `@MessageMapping` are provided by Sprout (not JSR-356).**

---

## 🧪 Testing

**Reports:** [Tests](https://<your-username>.github.io/sprout/tests/) ·
[Coverage](https://<your-username>.github.io/sprout/coverage/)

**575 tests, 0 failures (100% pass, Gradle 8.10.1 · 2025‑09‑23)**

**Test Coverage (Jacoco):**
- **Line Coverage: 85%**
- **Branch Coverage: 75%**

Coverage highlights:

* **Core container**: scanning, bean graph/topological sort, constructor injection, `@Order` list injection
* **AOP**: advice builders/interceptors, advisor registry, pointcut parsing
* **MVC layer**: request parsing (line/header/query), handler mapping & invocation, argument resolvers, exception advice
* **Security**: authentication providers, password encoding, context propagation, filters & authorization aspect
* **Server stack**: HTTP Blocking/NIO/Hybrid strategies, executor services (virtual vs pool), protocol detectors/handlers
* **WebSocket**: handshake, frame encoder/parser, ping/pong, fragmentation, async write & graceful close, dispatchers/resolvers
* **Utilities**: `HttpUtils` (Content-Length & chunked), response buffer creation, misc helpers

Tooling & style:

* JUnit 5 + Mockito (inline/lenient for JDK-final classes)
* Fake implementations where deterministic behavior beats heavy mocking (e.g., frame encoder/parser)
* Selector/`interestOps` state fakes to validate NIO behavior without real sockets
* Build report: `build/reports/tests/test/index.html`

> Want to help? Add black-box integration tests that spin up the NIO server and hit it with a real HTTP/WebSocket client.

## 🗺️ Roadmap

| Release  | Planned / Done                             | Notes                                           |
| -------- | ------------------------------------------ | ----------------------------------------------- |
| **v0.2** | ✅ AOP core delivered                       | `@Before`/`@After`/`@Around`, AspectJ pointcuts |
| **v0.3** | ✅ Middleware & Global Interceptors         | Filters + Interceptors chain                    |
| **v0.4** | ✅ Data Access & Security Core              | `JdbcTemplate`, `@Transactional`, AuthN/AuthZ   |
| **v0.5** | ✅ **NIO & Hybrid Server**, Async WebSocket | Selector loop, OP\_WRITE mgmt, graceful close   |
| **v1.0** | 🎯 **Stable API & Framework Maturity**     | Ready for production use, comprehensive docs    |

**Post v1.0 Roadmap:**  

| Feature  | Status | Description |
| -------- | ------ | ----------- |
| **Lightweight ORM** | 🔄 Planned | Entity mapping, annotations-based ORM, simple query DSL |
| **Production Tools** | 🔄 Planned | Metrics, monitoring, better performance profiling |
| **Advanced Features** | 🔄 Planned | Enhanced security, caching layer, validation framework |

> The roadmap is aspirational.

---

## 🙏 Acknowledgements

* **Spring Framework** — the architectural north star.
* **Reflections**, **CGLIB**, **Objenesis**, **Jackson** — runtime meta‑programming & serialization backbone.

---

## 🤝 Contributing

PRs & issues welcome. Pick a roadmap item or pitch a feature. Let’s grow Sprout together.

---

## 📜 License

MIT License. See [`LICENSE`](LICENSE).
