# 🏗️ IoC Container

The Inversion of Control (IoC) container is the heart of Sprout Framework. It manages object creation, dependency injection, and lifecycle for all components in your application.

## Overview

Sprout's IoC container provides:
- **Component Scanning**: Automatic detection of annotated classes using the Reflections library
- **Constructor Injection**: Type-safe dependency resolution (field injection not supported)
- **Lifecycle Management**: Bean creation, initialization, and destruction
- **Cyclic Dependency Detection**: Topological sorting and circular reference detection via BeanGraph
- **Order Support**: Controlling bean initialization and collection order with @Order
- **CGLIB Proxies**: Ensuring singleton behavior in @Configuration classes

## Container Architecture

### Core Components

Sprout's IoC container consists of the following key classes:

- `SproutApplicationContext`: Main application context
- `DefaultListableBeanFactory`: Core bean factory implementation
- `ClassPathScanner`: Classpath scanning and bean definition creation
- `BeanGraph`: Dependency graph and topological sorting

### Container Initialization Process

```java
public class SproutApplication {
    public static void run(Class<?> primarySource) throws Exception {
        // 1. Set up package scanning
        List<String> packages = getPackagesToScan(primarySource);
        
        // 2. Create application context
        ApplicationContext applicationContext = 
            new SproutApplicationContext(packages.toArray(new String[0]));
        
        // 3. Initialize context (refresh)
        applicationContext.refresh();
        
        // 4. Start server
        HttpServer server = applicationContext.getBean(HttpServer.class);
        server.start(port);
    }
}
```

## Component Scanning

### Supported Annotations

Sprout recognizes these component annotations:

```java
@Component         // Generic component
@Service          // Business logic layer
@Repository       // Data access layer
@Controller       // Web layer
@Configuration    // Configuration classes
@Aspect           // AOP aspects
@ControllerAdvice // Global exception handling
@WebSocketHandler // WebSocket handlers
```

### Scanning Process

```java
// ClassPathScanner's scanning logic
public Collection<BeanDefinition> scan(ConfigurationBuilder configBuilder, 
                                     Class<? extends Annotation>... componentAnnotations) {
    // 1. Annotation-based class discovery using Reflections
    Set<Class<?>> componentCandidates = new HashSet<>();
    for (Class<? extends Annotation> anno : componentAnnotations) {
        componentCandidates.addAll(r.getTypesAnnotatedWith(anno));
    }
    
    // 2. Filter to concrete classes only (exclude interfaces, abstract classes)
    Set<Class<?>> concreteComponentTypes = componentCandidates.stream()
        .filter(clazz -> !clazz.isInterface() && 
                        !clazz.isAnnotation() && 
                        !Modifier.isAbstract(clazz.getModifiers()))
        .collect(Collectors.toSet());
    
    // 3. @Bean method-based bean discovery
    Set<Class<?>> configClasses = r.getTypesAnnotatedWith(Configuration.class);
    for (Class<?> configClass : configClasses) {
        for (Method method : configClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Bean.class)) {
                beanMethodReturnTypes.add(method.getReturnType());
            }
        }
    }
}
```

### Enabling Component Scanning

Use `@ComponentScan` on your main application class:

```java
@ComponentScan("com.myapp")  // Scan specific package
@ComponentScan({"com.myapp.web", "com.myapp.service"})  // Multiple packages
public class Application {
    public static void main(String[] args) throws Exception {
        SproutApplication.run(Application.class);
    }
}
```

## Dependency Injection

### Constructor Injection Strategy

Sprout supports **constructor injection only**. It selects the constructor with the most parameters that can be resolved:

```java
// Constructor resolution logic
private Constructor<?> resolveUsableConstructor(Class<?> clazz, Set<Class<?>> allKnownBeanTypes) {
    return Arrays.stream(clazz.getDeclaredConstructors())
        .filter(constructor -> Arrays.stream(constructor.getParameterTypes())
            .allMatch(param -> isResolvable(param, allKnownBeanTypes)))
        .max(Comparator.comparingInt(Constructor::getParameterCount))
        .orElseThrow(() -> new NoSuchMethodException("No usable constructor"));
}
```

### Dependency Resolution Rules

```java
private boolean isResolvable(Class<?> paramType, Set<Class<?>> allKnownBeanTypes) {
    // 1. List types are always resolvable
    if (List.class.isAssignableFrom(paramType)) {
        return true;
    }
    
    // 2. Check infrastructure types (ApplicationContext, BeanFactory, etc.)
    if (isKnownInfrastructureType(paramType)) {
        return true;
    }
    
    // 3. Find assignable types among known bean types
    return allKnownBeanTypes.stream()
        .anyMatch(knownType -> paramType.isAssignableFrom(knownType));
}
```

### Example: Basic Dependency Injection

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
    public void handle(Event event) { /* Email processing */ }
}

@Component
@Order(2)
public class LogEventHandler implements EventHandler {
    public void handle(Event event) { /* Log processing */ }
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

### Collection Injection Processing Logic

```java
// DefaultListableBeanFactory's collection injection post-processing
protected void postProcessListInjections() {
    for (PendingListInjection pending : pendingListInjections) {
        Set<Object> uniqueBeansForList = new HashSet<>();
        for (Object bean : singletons.values()) {
            if (pending.getGenericType().isAssignableFrom(bean.getClass())) {
                uniqueBeansForList.add(bean);
            }
        }

        // Sort by @Order annotation
        List<Object> sortedBeansForList = uniqueBeansForList.stream()
            .sorted(Comparator.comparingInt(bean -> {
                Class<?> clazz = bean.getClass();
                Order order = clazz.getAnnotation(Order.class);
                return (order != null) ? order.value() : Integer.MAX_VALUE;
            }))
            .toList();

        pending.getListToPopulate().clear();
        pending.getListToPopulate().addAll(sortedBeansForList);
    }
}
```

## Bean Definitions and Creation

### Bean Definition Types

Sprout supports two bean creation approaches:

1. **Constructor-based beans** (`ConstructorBeanDefinition`)
2. **Factory method beans** (`MethodBeanDefinition`)

### Constructor-Based Beans

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

```java
@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/myapp");
        return new HikariDataSource(config);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
```

### @Configuration Proxies

`@Configuration` classes use CGLIB to create proxies that ensure singleton behavior:

```java
@Configuration(proxyBeanMethods = true)  // Default value
public class AppConfig {
    @Bean
    public ServiceA serviceA() {
        return new ServiceA(serviceB());  // Returns same serviceB instance
    }

    @Bean
    public ServiceB serviceB() {
        return new ServiceB();
    }
}
```

```java
// Proxy creation logic
if (def.isConfigurationClassProxyNeeded()) {
    Enhancer enhancer = new Enhancer();
    enhancer.setSuperclass(def.getType());
    enhancer.setCallback(new ConfigurationMethodInterceptor(this));
    beanInstance = enhancer.create(def.getConstructorArgumentTypes(), deps);
}
```

## Lifecycle Management

### Container Initialization Process

```java
@Override
public void refresh() throws Exception {
    // 1. Scan bean definitions
    scanBeanDefinitions();
    
    // 2. Create infrastructure beans first (BeanPostProcessor, etc.)
    instantiateInfrastructureBeans();
    
    // 3. Create application beans
    instantiateAllSingletons();
    
    // 4. Callbacks after context initialization complete
    List<ContextInitializer> contextInitializers = getAllBeans(ContextInitializer.class);
    for (ContextInitializer initializer : contextInitializers) {
        initializer.initializeAfterRefresh(this);
    }
}
```

### Bean Creation Order

Sprout uses `BeanGraph` to analyze the dependency graph and topologically sort beans for creation in the correct order:

```java
private void instantiateGroup(List<BeanDefinition> defs) {
    // Determine dependency order via topological sorting
    List<BeanDefinition> order = new BeanGraph(defs).topologicallySorted();
    
    // Create beans in order
    order.forEach(beanFactory::createBean);
    
    // Post-process collection injections
    beanFactory.postProcessListInjections();
}
```

### @Primary and Bean Selection

When multiple beans of the same type exist, use `@Primary` to specify priority:

```java
private String choosePrimary(Class<?> requiredType, Set<String> candidates) {
    // 1. Find beans with @Primary annotation
    List<String> primaries = candidates.stream()
        .filter(name -> {
            BeanDefinition d = beanDefinitions.get(name);
            return d != null && d.isPrimary();
        })
        .toList();

    if (primaries.size() == 1) return primaries.get(0);
    if (primaries.size() > 1)
        throw new RuntimeException("@Primary beans conflict for type " + requiredType.getName());

    return null;
}
```

### Bean Post Processing

```java
// Apply BeanPostProcessor after bean creation
Object processedBean = beanInstance;
for (BeanPostProcessor processor : beanPostProcessors) {
    Object result = processor.postProcessBeforeInitialization(def.getName(), processedBean);
    if (result != null) processedBean = result;
}
for (BeanPostProcessor processor : beanPostProcessors) {
    Object result = processor.postProcessAfterInitialization(def.getName(), processedBean);
    if (result != null) processedBean = result;
}
```

## Cyclic Dependency Detection

Sprout detects circular dependencies at startup through `BeanGraph`. When circular references are found, it throws an exception to halt application startup:

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

// Circular dependency detected during topological sorting, causing startup error
```

## Bean Registration and Lookup

### Type-Based Bean Mapping

```java
// Map bean names by type (including interfaces and superclasses)
private void registerInternal(String name, Object bean) {
    singletons.put(name, bean);

    Class<?> type = bean.getClass();
    primaryTypeToNameMap.putIfAbsent(type, name);
    typeToNamesMap.computeIfAbsent(type, k -> new HashSet<>()).add(name);

    // Register interfaces
    for (Class<?> iface : type.getInterfaces()) {
        primaryTypeToNameMap.putIfAbsent(iface, name);
        typeToNamesMap.computeIfAbsent(iface, k -> new HashSet<>()).add(name);
    }
    
    // Register superclasses
    for (Class<?> p = type.getSuperclass(); 
         p != null && p != Object.class; 
         p = p.getSuperclass()) {
        primaryTypeToNameMap.putIfAbsent(p, name);
        typeToNamesMap.computeIfAbsent(p, k -> new HashSet<>()).add(name);
    }
}
```

### Bean Lookup

```java
@Override
public <T> T getBean(Class<T> requiredType) {
    // 1. Check if bean already exists
    T bean = getIfPresent(requiredType);
    if (bean != null) return bean;

    // 2. Collect candidates
    Set<String> candidates = candidateNamesForType(requiredType);
    if (candidates.isEmpty()) {
        throw new RuntimeException("No bean of type " + requiredType.getName() + " found");
    }

    // 3. Select primary
    String primary = choosePrimary(requiredType, candidates);
    if (primary == null) {
        if (candidates.size() == 1) primary = candidates.iterator().next();
        else throw new RuntimeException("No unique bean of type " + requiredType.getName());
    }

    // 4. Create if necessary and return
    return (T) createIfNecessary(primary);
}
```

## Best Practices

### 1. Use Constructor Injection
```java
// Recommended: Constructor injection ensures immutability
@Service
public class UserService {
    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }
}
```

### 2. Interface-Based Design
```java
// Recommended: Program against interfaces
@Service
public class OrderService {
    private final PaymentProcessor paymentProcessor;

    public OrderService(PaymentProcessor paymentProcessor) {
        this.paymentProcessor = paymentProcessor;
    }
}

@Component
public class StripePaymentProcessor implements PaymentProcessor {
    // Implementation
}
```

### 3. Avoid Circular Dependencies
```java
// Recommended: Break cycles with events
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

### 4. Use @Order for Sequence Control
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

## Performance Optimization

### Eager vs Lazy Loading

Sprout creates all singleton beans at application startup by default. This provides several benefits:

- Early detection of configuration errors at startup
- Improved runtime performance
- Predictable memory usage

### Bean Scopes

Currently, Sprout supports only singleton scope, where each bean has exactly one instance throughout the application lifecycle.

## Extension Points

### BeanDefinitionRegistrar

Dynamically register custom bean definitions:

```java
public class MyFeatureAutoConfiguration implements BeanDefinitionRegistrar {
    @Override
    public Collection<BeanDefinition> registerAdditionalBeanDefinitions(
            Collection<BeanDefinition> existingDefs) {
        // Conditional bean registration logic
        return additionalBeans;
    }
}
```

### BeanPostProcessor

Intercept bean creation process for additional processing:

```java
@Component
public class TimingBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(String beanName, Object bean) {
        if (bean.getClass().isAnnotationPresent(Timed.class)) {
            return createTimingProxy(bean);
        }
        return bean;
    }
}
```

Sprout's IoC container is designed to be similar to Spring but with a simpler and more predictable structure. It supports only constructor injection and provides a clear bean lifecycle, making it easy to debug and understand.