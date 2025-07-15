# 🌱 **Sprout**

A lightweight Java web framework built from scratch to demystify **how Spring works under the hood**. While it proudly *reinvents the wheel (twice)*, **clarity · hackability · extensibility** remain its guiding values.

---

## ✨ Core Features
| Area | Status & Highlights                                                                                                                                                                                                                                                                                                                  |
| --- |--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **IoC / DI Container** | • Scans `@Component`, `@Service`, `@Controller`, `@Repository`, `@Configuration`, `@Aspect`.   <br/>• Constructor‑based injection.   <br/>• Automatic `List<T>` population.  <br/>• Cyclic‑dependency detection (topological sort).                                                                                                  |
| **Bean Definitions** | • `ConstructorBeanDefinition` & `MethodBeanDefinition`.  <br/>• Factory‑method or constructor strategy.  <br/>• **Ctor‑Meta cache** – enables safe proxying of beans with required‑args constructors.                                                                                                                                |
| **AOP (NEW in 0.2)** | • Annotation‑driven (`@Before`, `@After`, `@Around`).  <br/>• AspectJ‑style pointcuts (`*`, `..`, `?`).  <br/>• Advisor/Advice/Pointcut hierarchy inspired by Spring.  <br/>• CGLIB subclassing + Objenesis fallback → works even when beans lack a no‑arg constructor.  <br/>• Supports proxy‑per‑target & orderable advisor chain. |
| **Configuration Proxy** | CGLIB proxy for `@Configuration(proxyBeanMethods = true)` → caches repeated `@Bean` calls.                                                                                                                                                                                                                                           |
| **Web Layer** | • Declarative routing with `@GetMapping`, `@PostMapping`, … (pattern `{var}` support).  <br/>• ArgumentResolvers for `@PathVariable`, `@RequestParam`, `@RequestBody`, …. <br/>• `RequestDispatcher` binds → invokes → resolves (`ResponseEntity`, DTO, `void`).                                                                     |
| **Server** | **Config‑driven threading** → swap between virtual threads (Java 21) and platform‑thread pools via `application.yml`.                                                                                                                                                                                                                |
| **Filters & Interceptors** | • Servlet-style `Filter` chain support.  <br/>• Global filters (`AuthenticationFilter`, `AuthorizationFilter`, custom CORS, logging, etc).  <br/>• Middleware-style request interception.                                                                                                                                            |
| **Security (NEW)** | • Modular authentication system with `AuthenticationManager`, `AuthenticationProvider`, `UserDetailsService`.   <br/>• Username/password login via `AuthenticationFilter`.   <br/>• Authorization via `@PreAuthorize` (AOP based)  <br/>• `SecurityContextHolder` & `ThreadLocal` strategy.                                               |
| **Configuration** | YAML support with relaxed‑binding injection (`AppConfig`).                                                                                                                                                                                                                                                                           |
| **Exception Handling** | Built‑in HTTP exceptions (`BadRequest`, `MethodNotAllowed`, …).  <br/>• Global exception resolvers & mappers.                                                                                                                                                                                                                        |
| **Bootstrap** | One‑liner `SproutApplication.run()` sets up container *and* starts server.                                                                                                                                                                                                                                                           |
| **Data Access (NEW)** | • `JdbcTemplate` abstraction for SQL execution (query/update).  <br/>• HikariCP connection pool integration.  <br/>• AOP-driven `@Transactional` advice.  <br/>• `TransactionManager` abstraction with auto‑commit & rollback.  <br/>• Future-ready for lightweight ORM support.                                                     |
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

## 🗺️ Roadmap

| Release  | Planned / Done                                                            | Notes                                                                |
|----------| ------------------------------------------------------------------------- | -------------------------------------------------------------------- |
| **v0.2** | ✅ **AOP core delivered** (`@Before`/`@After`/`@Around`, AspectJ pointcuts). | ✔️ Done                                                              |
| **v0.3 (latest)** | ✅ **Middleware & Global Interceptors**                                      | ✔️ Done  |
| **v0.4** | **NIO Server**                                                            | Evaluate `java.nio.channels` + Loom structured concurrency.          |
| **v0.5** | **Data‑Access Layer**                                                     | Lightweight `JdbcTemplate` + transaction advice.                     |
| **v1.0** | **Production‑ready**                                                      | Stability hardening, docs & samples complete.                        |

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
