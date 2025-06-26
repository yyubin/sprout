# 🌱 **Sprout**

A lightweight Java web framework built from scratch to demystify how the Spring Framework works.
While it proudly “reinvents the wheel (twice),” **simplicity, readability, and extensibility** are its core values.

---

## ✨ Key Features

| Area                    | Current Status                                                                                                                                                                                                                                                                                                                                              |
| ----------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **IoC / DI Container**  | Scans `@Component`, `@Service`, `@Controller`, `@Repository`, `@Configuration` and manages them as singletons<br/>– Constructor-based injection<br/>– Automatic injection of `List<T>` beans<br/>– Cyclic-dependency detection via topological sort                                                                                                         |
| **Bean Definitions**    | `ConstructorBeanDefinition` / `MethodBeanDefinition`<br/>– Supports both constructor and factory-method instantiation strategies                                                                                                                                                                                                                            |
| **Configuration Proxy** | Applies CGLIB proxy when `@Configuration(proxyBeanMethods = true)` is used → caches repeated `@Bean` method calls                                                                                                                                                                                                                                           |
| **Web Layer**           | • `RequestMappingRegistry` + pattern matching (supports `{var}`)<br/>• `HandlerMethodScanner` for automatic controller registration<br/>• Argument resolvers for `@PathVariable`, `@RequestParam`, etc.<br/>• `RequestDispatcher` handles parsing, binding, invocation<br/>• `ResponseEntity` standardization & `ResponseResolver` auto-converts DTO/`void` |
| **Server**              | **Config-driven threading model**: switch between virtual threads (default) and a platform-thread pool via `application.yml`                                                                                                                                                                                                                                |
| **Configuration**       | **YAML support**: external configuration injection through `application.yml` (AppConfig)                                                                                                                                                                                                                                                                    |
| **Exception Handling**  | Built-in handling for custom exceptions (`BadRequestException`, `UnsupportedHttpMethod`, …)                                                                                                                                                                                                                                                                 |
| **Bootstrap**           | One-liner startup: `SproutApplication.run()` initializes the container and launches the server                                                                                                                                                                                                                                                              |

---

## 🏃‍♂️ Quick Start

1. **Clone and build**

```bash
git clone https://github.com/yyubin/sprout.git
cd sprout
./gradlew build
```

2. **Run the application**
   *CGLIB proxies require extra JVM flags on Java 17+.*
   *Virtual-thread mode needs Java 21+.*

```bash
java --add-opens=java.base/java.lang=ALL-UNNAMED \
     --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
     --add-opens=java.base/java.io=ALL-UNNAMED \
     --add-opens=java.base/java.util=ALL-UNNAMED \
     -jar build/libs/sprout.jar
```

3. **Example**

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
// app/TestController.java
@Controller
@RequestMapping("/api")
public class TestController {

    private final TestService service;

    public TestController(TestService service) {
        this.service = service;
    }

    // Returning a DTO → automatically wrapped in ResponseEntity.ok(...)
    @GetMapping("/hello/{id}")
    public MessageDto hello(@PathVariable Long id) {
        String message = service.greet(id);
        return new MessageDto(message);
    }

    // Returning ResponseEntity → full control over status & headers
    @PostMapping("/greetings")
    public ResponseEntity<MessageDto> createGreeting(@RequestBody GreetingRequest request) {
        MessageDto newGreeting = service.create(request);
        return ResponseEntity.created(newGreeting);   // 201 Created
    }
}
```

---

## 🗺️ Roadmap

| Milestone | Planned Feature        | Description                                                                      |
| --------- | ---------------------- | -------------------------------------------------------------------------------- |
| **v0.2**  | **AOP**                | Implement `@Around` advice using CGLIB proxies (logging, transactions, …)        |
| **v0.3**  | **Middleware Layer**   | Interceptor pipeline, global exception handling, `@Cacheable` annotation support |
| **v0.4**  | **NIO Server**         | Evaluate non-blocking I/O with `java.nio.channels`                               |
| **v0.5**  | **Data Access**        | Introduce a simple `JdbcTemplate` and `RowMapper`-style mini-ORM                 |
| **v1.0**  | **Production Release** | Stabilize & complete documentation                                               |

> ⚠️ *Subject to change — the project is primarily for learning.*

---

## 🙏 Acknowledgements

* **Spring Framework** — reference architecture & endless inspiration
* **Reflections**, **CGLIB**, **Jackson** — runtime metaprogramming & serialization backbone

---

## 🤝 Contributing

Pull requests are warmly welcome! Check out the roadmap, pick an open issue, or suggest a new enhancement — we’d love your help improving Sprout.

---

## 📜 License

Sprout is released under the **MIT License**. See the [`LICENSE`](LICENSE) file for details.
