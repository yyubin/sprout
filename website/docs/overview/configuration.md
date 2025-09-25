# ⚙️ Configuration

Sprout uses a YAML-based configuration system that's loaded at startup. The `AppConfig` class handles parsing and provides convenient access to configuration properties.

## Configuration File

Create `src/main/resources/application.yml` in your project:

```yaml
author: your-name

server:
  execution-mode: hybrid   # nio | hybrid | blocking (default: hybrid)
  thread-type: virtual     # virtual | platform (default: virtual)
  thread-pool-size: 150    # used when thread-type = platform

sprout:
  database:
    url: jdbc:mysql://localhost:3306/sprout
    username: root
    password: change-me

  security:
    enabled: true
    jwt:
      secret: your-jwt-secret-key
      expiration: 86400  # 24 hours in seconds
```

## Server Configuration

### Execution Modes

Sprout supports three execution modes to suit different use cases:

| Mode      | HTTP Handler            | WebSocket | Use Case                     |
|-----------|------------------------|-----------|------------------------------|
| `nio`     | NIO selector           | NIO       | High concurrency, low memory |
| `hybrid`  | Virtual/platform pool  | NIO       | Production (recommended)     |
| `blocking`| Platform threads       | N/A       | Debugging/learning           |

#### NIO Mode
```yaml
server:
  execution-mode: nio
```
- Single-threaded selector loop for all I/O
- Most memory-efficient for high connection counts
- Best for I/O-bound applications

#### Hybrid Mode (Recommended)
```yaml
server:
  execution-mode: hybrid
  thread-type: virtual
```
- HTTP requests handled by virtual threads or platform pool
- WebSocket connections handled by NIO selector
- Best balance of performance and resource usage

#### Blocking Mode
```yaml
server:
  execution-mode: blocking
  thread-type: platform
  thread-pool-size: 200
```
- Traditional one-thread-per-request model
- Easiest to debug and understand
- WebSocket not supported in this mode

### Thread Types

Choose between virtual threads (Project Loom) and traditional platform threads:

| Type            | When to Use                                    | Configuration |
|-----------------|------------------------------------------------|---------------|
| `virtual`       | Default choice (recommended)                   | `thread-type: virtual` |
| `platform`      | When you need bounded concurrency             | `thread-type: platform` + `thread-pool-size` |

```yaml
server:
  thread-type: virtual  # Unlimited virtual threads
```

```yaml
server:
  thread-type: platform
  thread-pool-size: 150  # Fixed-size thread pool
```

## Database Configuration

Configure your database connection:

```yaml
sprout:
  database:
    url: jdbc:mysql://localhost:3306/myapp
    username: myuser
    password: mypassword
```

### CORS Configuration
```yaml
cors:
  allow-origin: "*"
  allow-credentials: true
  allow-methods: "GET, POST, PUT, DELETE, OPTIONS"
  allow-headers: "Content-Type, Authorization"
  expose-headers: ""
  max-age: 3600
```
## Accessing Configuration in Code

Use `AppConfig` to access configuration values.

```java
@Component
public class MyService {
    private final AppConfig config;

    public MyService(AppConfig config) {
        this.config = config;
    }

    public void doSomething() {
        String author = config.getStringProperty("author");
        int poolSize = config.getIntProperty("server.thread-pool-size", 100);
        boolean securityEnabled = config.getBooleanProperty("sprout.security.enabled", false);
    }
}
```

## Configuration Validation

Sprout validates configuration at startup and provides helpful error messages:

```java
// Invalid configuration will cause startup failure
server:
  execution-mode: invalid-mode  # Error: Unknown execution mode
  thread-pool-size: -1          # Error: Pool size must be positive
```