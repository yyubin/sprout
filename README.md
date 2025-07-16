# 🌱 **Sprout**

A lightweight Java web framework built from scratch to demystify **how Spring works under the hood**. While it proudly *reinvents the wheel (twice)*, **clarity · hackability · extensibility** remain its guiding values.

---

## ✨ Core Features
| Area | Status & Highlights                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| --- |------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **IoC / DI Container** | • Scans `@Component`, `@Service`, `@Controller`, `@Repository`, `@Configuration`, `@Aspect`. <br/>• Constructor‑based injection. <br/>• Automatic `List<T>` population with `@Order` support. <br/>• Cyclic‑dependency detection (topological sort). <br/>• **Auto-configuration support via `BeanDefinitionRegistrar` for default beans.**                                                                                                                                                                                                                                              |
| **Bean Definitions** | • `ConstructorBeanDefinition` & `MethodBeanDefinition`. <br/>• Factory‑method or constructor strategy. <br/>• **Ctor‑Meta cache** – enables safe proxying of beans with required‑args constructors.                                                                                                                                                                                                                                                                                                                                                                                      |
| **AOP (NEW in 0.2)** | • Annotation‑driven (`@Before`, `@After`, `@Around`). <br/>• AspectJ‑style pointcuts (`*`, `..`, `?`).  <br/>• Advisor/Advice/Pointcut hierarchy inspired by Spring.  <br/>• CGLIB subclassing + Objenesis fallback → works even when beans lack a no‑arg constructor.  <br/>• Supports proxy‑per‑target & orderable advisor chain.                                                                                                                                                                                                                                                      |
| **Configuration Proxy** | CGLIB proxy for `@Configuration(proxyBeanMethods = true)` → caches repeated `@Bean` calls.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| **Web Layer** | • Declarative routing with `@GetMapping`, `@PostMapping`, … (pattern `{var}` support). <br/>• ArgumentResolvers for `@PathVariable`, `@RequestParam`, `@RequestBody`, …. <br/>• `RequestDispatcher` binds → invokes → resolves (`ResponseEntity`, DTO, `void`).                                                                                                                                                                                                                                                                                                                          |
| **Server** | **Config‑driven threading** → swap between virtual threads (Java 21) and platform‑thread pools via `application.yml`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| **Filters & Interceptors** | • Servlet-style `Filter` chain support. <br/>• Global filters (`AuthenticationFilter`, `AuthorizationFilter`, custom CORS, logging, etc.). <br/>• Middleware-style request interception (`Interceptor` chain). <br/>• **Automatic injection of `List<Filter>` and `List<Interceptor>` into `RequestDispatcher`.**                                                                                                                                                                                                                                                                        |
| **Security (NEW)** | • **Modular authentication system** with `AuthenticationManager`, `AuthenticationProvider`, `UserDetailsService`. <br/>• **Username/password login** via `AuthenticationFilter`. <br/>• **Authorization via `@PreAuthorize`** (AOP based, method-level security). <br/>• **URL-based authorization** via `AuthorizationFilter`. <br/>• `SecurityContextHolder` & `ThreadLocal` strategy for per-request security context. <br/>• **Auto-configuration for default security beans** (`@EnableSproutSecurity`).                                                                            |
| **Configuration** | YAML support with relaxed‑binding injection (`AppConfig`).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| **Exception Handling** | Built‑in HTTP exceptions (`BadRequest`, `MethodNotAllowed`, …). <br/>• **Global exception handling via `@ControllerAdvice` and `@ExceptionHandler`.**                                                                                                                                                                                                                                                                                                                                                         <br/>• **Extensible `ExceptionResolver` chain** for custom error handling. |
| **Bootstrap** | One‑liner `SproutApplication.run()` sets up container *and* starts server.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| **Data Access (NEW)** | • **Lightweight `JdbcTemplate` abstraction** for SQL execution (query/update).  <br/>• **HikariCP connection pool integration**.  <br/>• **AOP-driven `@Transactional` advice** for declarative transaction management.  <br/>• `TransactionManager` abstraction with auto‑commit & rollback.  <br/>• Future-ready for lightweight ORM support.                                                                                                                                                                                                                                          |
---

## 🏃‍♂️ Quick Start

1. **Clone & build**

```bash
$ git clone https://github.com/yyubin/sprout.git
$ cd sprout && ./gradlew build
```

2. **Run the sample app**  *(Java 21+, CGLIB module‑opens flags required)*

```bash
$ java \
  --add-opens=java.base/java.lang=ALL-UNNAMED \
  --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
  --add-opens=java.base/java.io=ALL-UNNAMED \
  --add-opens=java.base/java.util=ALL-UNNAMED \
  -jar build/libs/sprout.jar
```

3. **Minimal example**

```java
// DemoApplication.java
@ComponentScan("app")
public class DemoApplication {
    public static void main(String[] args) throws Exception {
        SproutApplication.run(DemoApplication.class);
    }
}
```

```java
// app/LoggingAspect.java
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
// app/TestController.java
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
### 🔌 WebSocket Support (Experimental)

Sprout now includes **low-level WebSocket server support**, implemented entirely over raw `Socket` I/O — no Tomcat, no Undertow, just **pure BIO** madness.

| Feature | Highlights |
| --- | --- |
| **HTTP Upgrade → WS handshake** | Parses HTTP handshake requests manually, responds with RFC 6455-compliant upgrade headers. |
| **`WebSocketSession` abstraction** | Track session state, send/receive messages, path/query parameter access. |
| **Frame-level protocol** | Encodes/decodes WebSocket frames manually: opcode routing, masking, fragmentation handling planned. |
| **Custom routing** | Messages are dispatched to `@MessageMapping` methods via `destination` field in incoming JSON. |
| **Argument resolution** | JSON payloads auto-bound via pluggable `WebSocketArgumentResolver`s. |
| **Session lifecycle hooks** | Supports `@OnOpen`, `@OnMessage`, `@OnClose`, `@OnError` via reflection-based `WebSocketEndpointInfo`. |
| **Pluggable frame codecs** | Swap `WebSocketFrameParser` / `WebSocketFrameEncoder` implementations for binary/custom handling. |
| **Thread model** | Session message loops respect configured thread model (`platform` or `virtual`). |

> Note: This implementation is BIO-based and suitable for educational purposes or light workloads.
>
>
> **NIO support** is planned for [v0.5](https://www.notion.so/WebSocket-23297bb367288011a487caf25d2b7543?pvs=21).
>

**Example:**

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

---

## 🗺️ Roadmap
| Release | Planned / Done | Notes |
| --- | --- | --- |
| **v0.2** | ✅ **AOP core delivered** (`@Before`/`@After`/`@Around`, AspectJ pointcuts). | ✔️ Done |
| **v0.3** | ✅ **Middleware & Global Interceptors** | ✔️ Done |
| **v0.4 (latest)** | ✅ **Data Access Layer & Security Core** | Includes `JdbcTemplate`, `@Transactional`, Authentication/Authorization. |
| **v0.5** | **NIO Server** | Evaluate `java.nio.channels` + Loom structured concurrency. |
| **v0.6** | **Lightweight ORM** | Basic Entity mapping and Query methods. |
| **v1.0** | **Production‑ready** | Stability hardening, docs & samples complete. |

*Roadmap is aspirational & may evolve as the learning journey continues.*

---

## 🙏 Acknowledgements

* **Spring Framework** — reference architecture & endless inspiration.
* **Reflections**, **CGLIB**, **Objenesis**, **Jackson** — runtime metaprogramming & serialization backbone.

---

## 🤝 Contributing

Pull requests and issue reports are very welcome! Pick a roadmap item or suggest your own — let’s grow Sprout together.

---

## 📜 License

Sprout is released under the **MIT License**. See the [`LICENSE`](LICENSE) file for details.
