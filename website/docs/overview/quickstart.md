# 🏃‍♂️ Quick Start

Get up and running with Sprout in just a few minutes!

## Prerequisites

- **Java 21+** (required for virtual threads and modern language features)
- **Gradle** (for building the project)

## 1. Clone & Build

```bash
$ git clone https://github.com/yyubin/sprout.git
$ cd sprout && ./gradlew build
```

## 2. Run the Application

:::info Java 21 Module Requirements
CGLIB proxies require additional module access flags for deep reflection:
:::

```bash
$ java \
  --add-opens=java.base/java.lang=ALL-UNNAMED \
  --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
  --add-opens=java.base/java.io=ALL-UNNAMED \
  --add-opens=java.base/java.util=ALL-UNNAMED \
  -jar build/libs/sprout.jar
```

:::tip
Server mode and thread model are configured via `application.yml`, not command-line arguments.
:::

## 3. Your First Application

### Main Application Class

```java
@ComponentScan("com.example.app")
public class DemoApplication {
    public static void main(String[] args) throws Exception {
        SproutApplication.run(DemoApplication.class);
    }
}
```

### Simple Controller

```java
@Controller
@RequestMapping("/api")
public class HelloController {
    private final GreetingService service;

    public HelloController(GreetingService service) {
        this.service = service;
    }

    @GetMapping("/hello/{id}")
    public MessageDto hello(@PathVariable Long id) {
        return new MessageDto(service.greet(id));
    }

    @PostMapping("/users")
    public ResponseEntity<User> createUser(@RequestBody CreateUserRequest request) {
        User user = service.createUser(request);
        return ResponseEntity.ok(user);
    }
}
```

### Service with AOP

```java
@Service
public class GreetingService {
    public String greet(Long id) {
        return "Hello, User " + id + "!";
    }

    public User createUser(CreateUserRequest request) {
        // Business logic here
        return new User(request.getName(), request.getEmail());
    }
}
```

### AOP Aspect

```java
@Aspect
public class LoggingAspect {
    @Around(pointcut = "com.example.app..*Service.*")
    public Object logExecutionTime(ProceedingJoinPoint pjp) throws Throwable {
        long startTime = System.nanoTime();
        try {
            return pjp.proceed();
        } finally {
            long duration = (System.nanoTime() - startTime) / 1_000;
            System.out.printf("%s took %d µs%n",
                pjp.getSignature().toLongName(), duration);
        }
    }
}
```

## 4. Configuration

Create `src/main/resources/application.yml`:

```yaml
author: your-name
server:
  execution-mode: hybrid   # nio | hybrid | blocking
  thread-type: virtual     # virtual | platform
  thread-pool-size: 150    # only when thread-type = platform

sprout:
  database:
    url: jdbc:mysql://localhost:3306/myapp
    username: root
    password: your-password
```

## 5. Test Your Application

Once running, test your endpoints:

```bash
# GET request
curl http://localhost:8080/api/hello/123

# POST request
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "John Doe", "email": "john@example.com"}'
```

## What's Next?

- 🏗️ [Learn the Architecture](../architecture/ioc-container) - Understand how Sprout works
- 🌐 [Build a Web App](../guides/building-web-app) - Step-by-step guide to building real applications
- 🔌 [WebSocket Chat](../guides/websocket-chat) - Real-time communication example
- 🔒 [Security Setup](../guides/security-setup) - Add authentication and authorization
- ⚙️ [Configuration Reference](../reference/configuration) - All configuration options

## Having Issues?

- Browse [Common Issues](https://github.com/yyubin/sprout/issues)
- Join the [Community Discussions](https://github.com/yyubin/sprout/discussions)