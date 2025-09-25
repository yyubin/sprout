# 🏗️ IoC Container

The Inversion of Control (IoC) container is the heart of Sprout Framework. It manages object creation, dependency injection, and lifecycle for all components in your application.

## Overview

Sprout's IoC container provides:
- **Component Scanning**: Automatic detection of annotated classes
- **Constructor Injection**: Type-safe dependency resolution
- **Lifecycle Management**: Bean creation, initialization, and destruction
- **Cyclic Dependency Detection**: Early detection of circular references
- **Order Support**: Controlling bean initialization and collection order

## Component Scanning

### Supported Annotations

Sprout recognizes these component annotations:

```java
@Component    // Generic component
@Service      // Business logic layer
@Repository   // Data access layer
@Controller   // Web layer
@Configuration // Configuration classes
@Aspect       // AOP aspects
```

### Enabling Component Scanning

Use `@ComponentScan` on your main application class:

```java
@ComponentScan("com.myapp")  // Scan specific package
@ComponentScan({"com.myapp.web", "com.myapp.service"})  // Multiple packages
public class Application {
    public static void main(String[] args) {
        SproutApplication.run(Application.class);
    }
}
```

## Dependency Injection

### Constructor Injection

Sprout uses constructor injection exclusively for better immutability and testability:

```java
@Service
public class UserService {
    private final UserRepository repository;
    private final EmailService emailService;

    // Constructor injection - no @Autowired needed
    public UserService(UserRepository repository, EmailService emailService) {
        this.repository = repository;
        this.emailService = emailService;
    }
}

@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
}
```

### Collection Injection

Inject all beans of a specific type as a `List`:

```java
public interface EventHandler {
    void handle(Event event);
}

@Component
@Order(1)
public class EmailEventHandler implements EventHandler {
    public void handle(Event event) { /* ... */ }
}

@Component
@Order(2)
public class LogEventHandler implements EventHandler {
    public void handle(Event event) { /* ... */ }
}

@Service
public class EventProcessor {
    private final List<EventHandler> handlers;

    // All EventHandler beans injected in @Order sequence
    public EventProcessor(List<EventHandler> handlers) {
        this.handlers = handlers;
    }

    public void processEvent(Event event) {
        handlers.forEach(handler -> handler.handle(event));
    }
}
```

## Bean Definitions

### Constructor-Based Beans

Most beans are created using their constructors:

```java
@Component
public class NotificationService {
    private final EmailService emailService;
    private final SmsService smsService;

    public NotificationService(EmailService emailService, SmsService smsService) {
        this.emailService = emailService;
        this.smsService = smsService;
    }
}
```

### Factory Method Beans

Use `@Bean` methods in `@Configuration` classes:

```java
@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/myapp");
        config.setUsername("user");
        config.setPassword("password");
        return new HikariDataSource(config);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
```

### Configuration Proxies

`@Configuration` classes support method interception for singleton behavior:

```java
@Configuration(proxyBeanMethods = true)  // Default behavior
public class AppConfig {

    @Bean
    public ServiceA serviceA() {
        return new ServiceA(serviceB());  // Returns same serviceB instance
    }

    @Bean
    public ServiceB serviceB() {
        return new ServiceB();
    }

    @Bean
    public ServiceC serviceC() {
        return new ServiceC(serviceB());  // Returns same serviceB instance
    }
}
```

## Lifecycle Management

### Bean Creation Order

Sprout creates beans in topologically sorted order to respect dependencies:

```java
@Component
@Order(1)
public class DatabaseMigrator {
    // Runs first due to @Order(1)
    @PostConstruct
    public void migrate() {
        // Perform database migrations
    }
}

@Component
@Order(2)
public class CacheWarmer {
    private final UserService userService;

    // UserService is available because it has no @Order (default: Integer.MAX_VALUE)
    public CacheWarmer(UserService userService) {
        this.userService = userService;
    }

    @PostConstruct
    public void warmCache() {
        // Pre-load frequently used data
    }
}
```

### Initialization Callbacks

```java
@Component
public class MyService implements InitializingBean, DisposableBean {

    @Override
    public void afterPropertiesSet() throws Exception {
        // Called after dependency injection
        System.out.println("MyService initialized");
    }

    @Override
    public void destroy() throws Exception {
        // Called during shutdown
        System.out.println("MyService destroyed");
    }
}

// Alternative: use annotations
@Component
public class AnotherService {

    @PostConstruct
    public void init() {
        System.out.println("AnotherService initialized");
    }

    @PreDestroy
    public void cleanup() {
        System.out.println("AnotherService destroyed");
    }
}
```

## Cyclic Dependency Detection

Sprout detects circular dependencies at startup:

```java
@Component
public class ServiceA {
    public ServiceA(ServiceB serviceB) { /* ... */ }
}

@Component
public class ServiceB {
    public ServiceB(ServiceC serviceC) { /* ... */ }
}

@Component
public class ServiceC {
    public ServiceC(ServiceA serviceA) { /* ... */ }  // Circular dependency!
}

// Results in startup error with clear dependency chain:
// Circular dependency detected: ServiceA -> ServiceB -> ServiceC -> ServiceA
```

## Auto-Configuration

Sprout provides auto-configuration for common scenarios:

### Built-in Auto-Configurations

```java
@EnableSproutSecurity  // Enables security auto-configuration
@EnableTransactionManagement  // Enables @Transactional support
@EnableAsync  // Enables @Async support
public class Application {
    // Auto-configured beans are available:
    // - SecurityContext, AuthenticationManager
    // - TransactionManager, PlatformTransactionManager
    // - TaskExecutor, AsyncTaskExecutor
}
```

### Custom Auto-Configuration

Implement `BeanDefinitionRegistrar` for custom auto-configuration:

```java
public class MyFeatureAutoConfiguration implements BeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(BeanDefinitionRegistry registry) {
        if (isMyFeatureEnabled()) {
            registry.registerBeanDefinition("myFeatureService",
                new ConstructorBeanDefinition(MyFeatureService.class));
        }
    }

    private boolean isMyFeatureEnabled() {
        // Check configuration, classpath, etc.
        return true;
    }
}
```

## Container Internals

### Bean Registry

The container maintains several registries:

```java
public class SproutApplicationContext {
    private final Map<String, Object> singletonBeans;
    private final Map<Class<?>, List<String>> beansByType;
    private final Map<String, BeanDefinition> beanDefinitions;
    private final List<BeanPostProcessor> beanPostProcessors;
}
```

### Bean Post-Processors

Extend bean creation with custom logic:

```java
@Component
public class TimingBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean.getClass().isAnnotationPresent(Timed.class)) {
            return createTimingProxy(bean);
        }
        return bean;
    }

    private Object createTimingProxy(Object target) {
        // Create CGLIB proxy that times method calls
        return proxyFactory.createProxy(target, new TimingInterceptor());
    }
}
```

## Best Practices

### 1. Prefer Constructor Injection
```java
// Good: Constructor injection
@Service
public class UserService {
    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }
}

// Avoid: Field injection (not supported in Sprout anyway)
```

### 2. Use Interfaces for Loose Coupling
```java
// Good: Program against interfaces
@Service
public class OrderService {
    private final PaymentProcessor paymentProcessor;

    public OrderService(PaymentProcessor paymentProcessor) {
        this.paymentProcessor = paymentProcessor;
    }
}

// Implementation
@Component
public class StripePaymentProcessor implements PaymentProcessor {
    // Implementation details
}
```

### 3. Avoid Circular Dependencies
```java
// Good: Break cycles with events or interfaces
@Service
public class OrderService {
    private final EventPublisher eventPublisher;

    public void processOrder(Order order) {
        // Process order
        eventPublisher.publish(new OrderProcessedEvent(order));
    }
}

@Component
public class InventoryService {
    @EventListener
    public void handleOrderProcessed(OrderProcessedEvent event) {
        // Update inventory
    }
}
```

### 4. Use @Order for Collection Dependencies
```java
@Component
@Order(1)
public class ValidationFilter implements Filter {
    // Runs first
}

@Component
@Order(2)
public class AuthenticationFilter implements Filter {
    // Runs after validation
}
```

The IoC container is designed to be predictable, debuggable, and efficient. Understanding these concepts will help you build well-structured Sprout applications.