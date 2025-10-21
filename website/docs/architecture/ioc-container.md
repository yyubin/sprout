# 🏗️ IoC Container

The Inversion of Control (IoC) container is the core of the Sprout Framework. It manages the creation, dependency injection, and lifecycle of all application components.

## Overview

Sprout’s IoC container provides the following features:
- **Component Scanning**: Automatic detection of classes based on annotations using the Reflections library.
- **Constructor Injection**: Type-safe dependency resolution (field injection is not supported).
- **Lifecycle Management**: Phased bean creation, initialization, and destruction.
- **Circular Dependency Detection**: Topological sorting and cycle detection via BeanGraph.
- **Order Support**: Control of bean initialization and collection ordering with `@Order`.
- **CGLIB Proxy**: Ensures singleton behavior for `@Configuration` classes.
- **Extensibility via Strategy Pattern**: Plugin-based structure for bean creation and dependency resolution strategies.

## Container Architecture

### Core Components

Sprout’s IoC container consists of the following key classes:

#### Context and Factory
- `SproutApplicationContext`: The main application context.
- `DefaultListableBeanFactory`: The core bean factory implementation.
- `ClassPathScanner`: Scans the classpath and generates bean definitions.
- `BeanGraph`: Manages dependency graphs and topological sorting.

#### Bean Creation Strategies (Strategy Pattern)
- `BeanInstantiationStrategy`: Interface for bean instantiation strategies.
    - `ConstructorBasedInstantiationStrategy`: Creates beans via constructors.
    - `FactoryMethodBasedInstantiationStrategy`: Creates beans via factory methods.

#### Dependency Resolution Strategies (Chain of Responsibility Pattern)
- `DependencyResolver`: Interface for dependency resolution.
    - `CompositeDependencyResolver`: Combines multiple resolvers.
- `DependencyTypeResolver`: Strategy for resolving dependencies by type.
    - `SingleBeanDependencyResolver`: Resolves single bean dependencies.
    - `ListBeanDependencyResolver`: Resolves `List` type dependencies.

#### Lifecycle Management (Phase Pattern)
- `BeanLifecycleManager`: Manages the execution of lifecycle phases.
- `BeanLifecyclePhase`: Interface for lifecycle phases.
    - `InfrastructureBeanPhase`: Creates infrastructure beans (order=100).
    - `BeanPostProcessorRegistrationPhase`: Registers BeanPostProcessors (order=200).
    - `ApplicationBeanPhase`: Creates application beans (order=300).
    - `ContextInitializerPhase`: Executes ContextInitializers (order=400).

#### Type Matching Service
- `BeanTypeMatchingService`: Centralizes logic for type-based bean searching and matching.

### Container Initialization Process

```java
public class SproutApplication {
    public static void run(Class<?> primarySource) throws Exception {
        // 1. Configure packages to scan
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

Sprout recognizes the following component annotations:

```java
@Component         // General component
@Service          // Business logic layer
@Repository       // Data access layer
@Controller       // Web layer
@Configuration    // Configuration class
@Aspect           // AOP aspect
@ControllerAdvice // Global exception handling
@WebSocketHandler // WebSocket handler
```

### Scanning Process

```java
// Scanning logic in ClassPathScanner
public Collection<BeanDefinition> scan(ConfigurationBuilder configBuilder, 
                                     Class<? extends Annotation>... componentAnnotations) {
    // 1. Find classes with annotations using Reflections
    Set<Class<?>> componentCandidates = new HashSet<>();
    for (Class<? extends Annotation> anno : componentAnnotations) {
        componentCandidates.addAll(r.getTypesAnnotatedWith(anno));
    }
    
    // 2. Filter concrete classes (exclude interfaces and abstract classes)
    Set<Class<?>> concreteComponentTypes = componentCandidates.stream()
        .filter(clazz -> !clazz.isInterface() && 
                        !clazz.isAnnotation() && 
                        !Modifier.isAbstract(clazz.getModifiers()))
        .collect(Collectors.toSet());
    
    // 3. Find beans defined by @Bean methods
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

Use `@ComponentScan` on the main application class:

```java
@ComponentScan("com.myapp")  // Scan a specific package
@ComponentScan({"com.myapp.web", "com.myapp.service"})  // Scan multiple packages
public class Application {
    public static void main(String[] args) throws Exception {
        SproutApplication.run(Application.class);
    }
}
```

## Dependency Injection

### Constructor Injection Strategy

Sprout supports **constructor injection only**. It selects the constructor with the most resolvable parameters.

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

### Dependency Resolution Architecture

Since Sprout v2.0, dependency resolution uses the **Chain of Responsibility Pattern** to significantly improve extensibility.

#### DependencyResolver Structure

```java
// Dependency resolution interface
public interface DependencyResolver {
    Object[] resolve(Class<?>[] dependencyTypes, Parameter[] params, BeanDefinition targetDef);
}

// Type-specific dependency resolution strategy
public interface DependencyTypeResolver {
    boolean supports(Class<?> type);
    Object resolve(Class<?> type, Parameter param, BeanDefinition targetDef);
}
```

#### Default Resolvers

1. **ListBeanDependencyResolver**: Handles `List` type dependencies.
    - Detects `List` parameters and creates an empty list.
    - Extracts generic type information and registers it in a pending list.
    - Later injects actual beans in `postProcessListInjections()`.

2. **SingleBeanDependencyResolver**: Handles single bean dependencies.
    - Retrieves beans from the BeanFactory for non-`List` types.
    - Uses type matching and `@Primary` selection logic.

#### CompositeDependencyResolver

Chains multiple `DependencyTypeResolver` instances for sequential processing:

```java
public class CompositeDependencyResolver implements DependencyResolver {
    private final List<DependencyTypeResolver> typeResolvers;

    @Override
    public Object[] resolve(Class<?>[] dependencyTypes, Parameter[] params, BeanDefinition targetDef) {
        Object[] deps = new Object[dependencyTypes.length];

        for (int i = 0; i < dependencyTypes.length; i++) {
            Class<?> paramType = dependencyTypes[i];
            Parameter param = params[i];

            // Find appropriate resolver and resolve dependency
            for (DependencyTypeResolver resolver : typeResolvers) {
                if (resolver.supports(paramType)) {
                    deps[i] = resolver.resolve(paramType, param, targetDef);
                    break;
                }
            }
        }
        return deps;
    }
}
```

#### Extending Dependency Resolution

To support new dependency types (e.g., `Optional`, `Provider`), implement `DependencyTypeResolver` and add it to the `DefaultListableBeanFactory` constructor:

```java
public class OptionalBeanDependencyResolver implements DependencyTypeResolver {
    @Override
    public boolean supports(Class<?> type) {
        return Optional.class.isAssignableFrom(type);
    }

    @Override
    public Object resolve(Class<?> type, Parameter param, BeanDefinition targetDef) {
        // Optional handling logic
        Class<?> genericType = extractGenericType(param);
        try {
            Object bean = beanFactory.getBean(genericType);
            return Optional.of(bean);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
```

### Example: Basic Dependency Injection

```java
@Service
public class UserService {
    private final UserRepository repository;
    private final EmailService emailService;

    // Constructor injection - @Autowired not required
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

You can inject all beans of a specific type as a `List`:

```java
public interface EventHandler {
    void handle(Event event);
}

@Component
@Order(1)
public class EmailEventHandler implements EventHandler {
    public void handle(Event event) { /* Email handling */ }
}

@Component
@Order(2)
public class LogEventHandler implements EventHandler {
    public void handle(Event event) { /* Log handling */ }
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

### Collection Injection Logic

```java
// Collection injection post-processing in DefaultListableBeanFactory
protected void postProcessListInjections() {
    for (PendingListInjection pending : pendingListInjections) {
        Set<Object> uniqueBeansForList = new HashSet<>();
        for (Object bean : singletons.values()) {
            if (pending.getGenericType().isAssignableFrom(bean.getClass())) {
                uniqueBeansForList.add(bean);
            }
        }

        // Sort based on @Order annotation
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

## Bean Definition and Creation

### Bean Definition Types

Sprout supports two types of bean creation:

1. **Constructor-Based Beans** (`ConstructorBeanDefinition`)
2. **Factory Method Beans** (`MethodBeanDefinition`)

### Bean Instantiation Strategies (Strategy Pattern)

Since Sprout v2.0, bean creation logic uses the **Strategy Pattern** to support various creation methods.

#### BeanInstantiationStrategy Interface

```java
public interface BeanInstantiationStrategy {
    Object instantiate(BeanDefinition def, DependencyResolver dependencyResolver, BeanFactory beanFactory) throws Exception;
    boolean supports(BeanCreationMethod method);
}
```

#### Implementations

**1. ConstructorBasedInstantiationStrategy**

Handles bean creation via constructors:

```java
public class ConstructorBasedInstantiationStrategy implements BeanInstantiationStrategy {
    @Override
    public Object instantiate(BeanDefinition def, DependencyResolver dependencyResolver, BeanFactory beanFactory) throws Exception {
        Constructor<?> constructor = def.getConstructor();

        // Resolve dependencies
        Object[] deps = dependencyResolver.resolve(
            def.getConstructorArgumentTypes(),
            constructor.getParameters(),
            def
        );

        // Create CGLIB proxy for Configuration classes
        if (def.isConfigurationClassProxyNeeded()) {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(def.getType());
            enhancer.setCallback(new ConfigurationMethodInterceptor(beanFactory));
            return enhancer.create(def.getConstructorArgumentTypes(), deps);
        } else {
            constructor.setAccessible(true);
            return constructor.newInstance(deps);
        }
    }

    @Override
    public boolean supports(BeanCreationMethod method) {
        return method == BeanCreationMethod.CONSTRUCTOR;
    }
}
```

**2. FactoryMethodBasedInstantiationStrategy**

Handles bean creation via factory methods (`@Bean`):

```java
public class FactoryMethodBasedInstantiationStrategy implements BeanInstantiationStrategy {
    @Override
    public Object instantiate(BeanDefinition def, DependencyResolver dependencyResolver, BeanFactory beanFactory) throws Exception {
        Object factoryBean = beanFactory.getBean(def.getFactoryBeanName());
        Method factoryMethod = def.getFactoryMethod();

        Object[] deps = dependencyResolver.resolve(
            def.getFactoryMethodArgumentTypes(),
            factoryMethod.getParameters(),
            def
        );

        factoryMethod.setAccessible(true);
        return factoryMethod.invoke(factoryBean, deps);
    }

    @Override
    public boolean supports(BeanCreationMethod method) {
        return method == BeanCreationMethod.FACTORY_METHOD;
    }
}
```

#### Strategy Usage in DefaultListableBeanFactory

```java
public class DefaultListableBeanFactory implements BeanFactory, BeanDefinitionRegistry {
    private final List<BeanInstantiationStrategy> instantiationStrategies;
    private final DependencyResolver dependencyResolver;

    public DefaultListableBeanFactory() {
        // Initialize strategies
        this.instantiationStrategies = new ArrayList<>();
        this.instantiationStrategies.add(new ConstructorBasedInstantiationStrategy());
        this.instantiationStrategies.add(new FactoryMethodBasedInstantiationStrategy());

        // Initialize dependency resolver
        List<DependencyTypeResolver> typeResolvers = new ArrayList<>();
        typeResolvers.add(new ListBeanDependencyResolver(pendingListInjections));
        typeResolvers.add(new SingleBeanDependencyResolver(this));
        this.dependencyResolver = new CompositeDependencyResolver(typeResolvers);
    }

    public Object createBean(BeanDefinition def) {
        // Select appropriate strategy
        BeanInstantiationStrategy strategy = findStrategy(def);

        // Create bean using strategy
        Object beanInstance = strategy.instantiate(def, dependencyResolver, this);

        // Apply BeanPostProcessors
        Object processedBean = applyBeanPostProcessors(beanInstance, def.getName());

        return processedBean;
    }

    private BeanInstantiationStrategy findStrategy(BeanDefinition def) {
        for (BeanInstantiationStrategy strategy : instantiationStrategies) {
            if (strategy.supports(def.getCreationMethod())) {
                return strategy;
            }
        }
        throw new IllegalArgumentException("No strategy found for: " + def.getCreationMethod());
    }
}
```

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

### @Configuration Proxy

`@Configuration` classes use CGLIB to create proxies, ensuring singleton behavior:

```java
@Configuration(proxyBeanMethods = true)  // Default
public class AppConfig {
    @Bean
    public ServiceA serviceA() {
        return new ServiceA(serviceB());  // Returns the same serviceB instance
    }

    @Bean
    public ServiceB serviceB() {
        return new ServiceB();
    }
}
```

## Lifecycle Management

Since Sprout v2.0, the **Phase Pattern** has been introduced to clearly separate and manage bean lifecycles.

### Container Initialization Process (Post-Refactoring)

```java
@Override
public void refresh() throws Exception {
    // 1. Scan bean definitions
    scanBeanDefinitions();

    // 2. Execute phases via BeanLifecycleManager
    BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
            beanFactory,
            infraDefs,
            appDefs,
            basePackages
    );

    lifecycleManager.executePhases(context);
}
```

The complex method calls from the previous version (`instantiateInfrastructureBeans()`, `instantiateAllSingletons()`, etc.) have been encapsulated into Phases, reducing the code from **19 lines to 10 lines**.

### BeanLifecyclePhase Interface

Represents each lifecycle phase:

```java
public interface BeanLifecyclePhase {
    String getName();
    int getOrder();
    void execute(PhaseContext context) throws Exception;

    class PhaseContext {
        private final BeanFactory beanFactory;
        private final List<BeanDefinition> infraDefs;
        private final List<BeanDefinition> appDefs;
        private final List<String> basePackages;
        // getters...
    }
}
```

### Lifecycle Phases

#### 1. InfrastructureBeanPhase (order=100)

Creates infrastructure beans (`BeanPostProcessor`, `InfrastructureBean`) first:

```java
public class InfrastructureBeanPhase implements BeanLifecyclePhase {
    @Override
    public void execute(PhaseContext context) throws Exception {
        DefaultListableBeanFactory factory = (DefaultListableBeanFactory) context.getBeanFactory();

        // Create beans in topological order
        List<BeanDefinition> order = new BeanGraph(context.getInfraDefs()).topologicallySorted();
        order.forEach(factory::createBean);

        // Post-process List injections
        factory.postProcessListInjections();

        // Execute PostInfrastructureInitializer
        List<PostInfrastructureInitializer> initializers =
            factory.getAllBeans(PostInfrastructureInitializer.class);
        for (PostInfrastructureInitializer initializer : initializers) {
            initializer.afterInfrastructureSetup(factory, context.getBasePackages());
        }
    }

    @Override
    public int getOrder() { return 100; }
}
```

#### 2. BeanPostProcessorRegistrationPhase (order=200)

Registers all `BeanPostProcessor` instances in the BeanFactory:

```java
public class BeanPostProcessorRegistrationPhase implements BeanLifecyclePhase {
    @Override
    public void execute(PhaseContext context) {
        DefaultListableBeanFactory factory = (DefaultListableBeanFactory) context.getBeanFactory();

        List<BeanPostProcessor> allBeanPostProcessor =
            factory.getAllBeans(BeanPostProcessor.class);

        for (BeanPostProcessor beanPostProcessor : allBeanPostProcessor) {
            factory.addBeanPostProcessor(beanPostProcessor);
        }
    }

    @Override
    public int getOrder() { return 200; }
}
```

#### 3. ApplicationBeanPhase (order=300)

Creates application beans:

```java
public class ApplicationBeanPhase implements BeanLifecyclePhase {
    @Override
    public void execute(PhaseContext context) {
        DefaultListableBeanFactory factory = (DefaultListableBeanFactory) context.getBeanFactory();

        // Create beans in topological order
        List<BeanDefinition> order = new BeanGraph(context.getAppDefs()).topologicallySorted();
        order.forEach(factory::createBean);

        // Post-process List injections
        factory.postProcessListInjections();
    }

    @Override
    public int getOrder() { return 300; }
}
```

#### 4. ContextInitializerPhase (order=400)

Executes all `ContextInitializer` instances:

```java
public class ContextInitializerPhase implements BeanLifecyclePhase {
    @Override
    public void execute(PhaseContext context) {
        BeanFactory beanFactory = context.getBeanFactory();

        List<ContextInitializer> contextInitializers =
            beanFactory.getAllBeans(ContextInitializer.class);
        for (ContextInitializer initializer : contextInitializers) {
            initializer.initializeAfterRefresh(beanFactory);
        }
    }

    @Override
    public int getOrder() { return 400; }
}
```

### BeanLifecycleManager

Manages the execution of all phases in order:

```java
public class BeanLifecycleManager {
    private final List<BeanLifecyclePhase> phases;

    public BeanLifecycleManager(List<BeanLifecyclePhase> phases) {
        this.phases = phases.stream()
                .sorted(Comparator.comparingInt(BeanLifecyclePhase::getOrder))
                .toList();
    }

    public void executePhases(BeanLifecyclePhase.PhaseContext context) throws Exception {
        for (BeanLifecyclePhase phase : phases) {
            System.out.println("--- Executing Phase: " + phase.getName() +
                " (order=" + phase.getOrder() + ") ---");
            phase.execute(context);
        }
    }
}
```

### Extending the Lifecycle

To add a new phase, implement `BeanLifecyclePhase` and register it in the `SproutApplicationContext` constructor:

```java
public class CustomPhase implements BeanLifecyclePhase {
    @Override
    public String getName() {
        return "Custom Initialization Phase";
    }

    @Override
    public int getOrder() {
        return 250;  // After BeanPostProcessor registration, before application beans
    }

    @Override
    public void execute(PhaseContext context) throws Exception {
        // Custom initialization logic
    }
}

// In SproutApplicationContext constructor
List<BeanLifecyclePhase> phases = new ArrayList<>();
phases.add(new InfrastructureBeanPhase());
phases.add(new BeanPostProcessorRegistrationPhase());
phases.add(new CustomPhase());  // Add custom phase
phases.add(new ApplicationBeanPhase());
phases.add(new ContextInitializerPhase());
this.lifecycleManager = new BeanLifecycleManager(phases);
```

### Bean Creation Order

Within each phase, `BeanGraph` analyzes the dependency graph and performs topological sorting to create beans in the correct order:

```java
// Determine order with topological sorting
List<BeanDefinition> order = new BeanGraph(defs).topologicallySorted();

// Create beans in order
order.forEach(beanFactory::createBean);

// Post-process collection injections
beanFactory.postProcessListInjections();
```

### @Primary and Bean Selection

When multiple beans of the same type exist, `@Primary` can specify priority:

```java
private String choosePrimary(Class<?> requiredType, Set<String> candidates) {
    // 1. Find beans with @Primary
    List<String> primaries = candidates.stream()
        .filter(name -> {
            BeanDefinition d = beanDefinitions.get(name);
            return d != null && d.isPrimary();
        })
        .toList();

    if (primaries.size() == 1) return primaries.get(0);
    if ( primaries.size() > 1)
        throw new RuntimeException("@Primary beans conflict for type " + requiredType.getName());

    return null;
}
```

### Bean Post-Processing

```java
// Apply BeanPostProcessors after bean creation
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

## Circular Dependency Detection

Sprout detects circular dependencies at startup using `BeanGraph`. If a cycle is detected, an exception is thrown, halting application startup:

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

// Topological sorting detects the cycle and throws an error
```

## Bean Registration and Retrieval

### Type-Based Bean Mapping

```java
// Map beans by type (including interfaces and superclasses)
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

### Bean Retrieval

```java
@Override
public <T> T getBean(Class<T> requiredType) {
    // 1. Check for existing bean
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

    // 4. Create and return if necessary
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
// Recommended: Depend on interfaces
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

### 4. Control Order with @Order
```java
@Component
@Order(1)
public class ValidationFilter implements Filter {
    // Executes first
}

@Component
@Order(2)
public class AuthenticationFilter implements Filter {
    // Executes after validation
}
```

## Performance Optimization

### Lazy vs. Eager Loading

Sprout creates all singleton beans at application startup by default, providing:
- Early detection of configuration errors.
- Improved runtime performance.
- Predictable memory usage.

### Bean Scopes

Currently, Sprout supports only singleton scope, where each bean has a single instance throughout the application.

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

Intervene in the bean creation process for additional processing:

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

## Architecture Refactoring Summary (v2.0)

### Motivation for Changes

Sprout v1.x IoC container had the following limitations:
- `DefaultListableBeanFactory` had too many responsibilities (SRP violation).
- Bean creation logic was centralized in a single method, making extension difficult.
- Dependency resolution logic was rigid, complicating new type additions.
- Lifecycle management was hardcoded, making new phase additions complex.
- Type matching logic was duplicated (BeanGraph vs. BeanFactory).

### Applied Design Patterns

#### 1. Strategy Pattern (Bean Creation)

**Before:**
```java
// All creation logic in createBean (50+ lines)
if (def.getCreationMethod() == BeanCreationMethod.CONSTRUCTOR) {
    // Constructor logic
} else if (def.getCreationMethod() == BeanCreationMethod.FACTORY_METHOD) {
    // Factory method logic
}
```

**After:**
```java
// Separated with Strategy Pattern
BeanInstantiationStrategy strategy = findStrategy(def);
Object beanInstance = strategy.instantiate(def, dependencyResolver, this);
```

**Benefits:**
- Easy to add new creation methods (e.g., builder pattern, static factory).
- Each strategy can be tested independently.
- Adheres to OCP (Open-Closed Principle).

#### 2. Chain of Responsibility Pattern (Dependency Resolution)

**Before:**
```java
// if-else branches in resolveDependencies
if (List.class.isAssignableFrom(paramType)) {
    // List handling
} else {
    // Single bean handling
}
```

**After:**
```java
// Sequential processing with resolver chain
for (DependencyTypeResolver resolver : typeResolvers) {
    if (resolver.supports(paramType)) {
        return resolver.resolve(paramType, param, targetDef);
    }
}
```

**Benefits:**
- Easy to support new types like `Optional` or `Provider`.
- Each resolver can be implemented and tested independently.
- Maximized extensibility.

#### 3. Phase Pattern (Lifecycle Management)

**Before:**
```java
// Hardcoded sequence in refresh() (19 lines)
scanBeanDefinitions();
instantiateInfrastructureBeans();
instantiateAllSingletons();
// ContextInitializer execution...
```

**After:**
```java
// Simplified with Phase Pattern (10 lines)
scanBeanDefinitions();
BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(...);
lifecycleManager.executePhases(context);
```

**Benefits:**
- Easy to add new lifecycle phases.
- Clear separation of responsibilities per phase.
- Improved testability and debugging.

#### 4. Service Separation (Type Matching)

**Before:**
- Duplicated logic in `BeanGraph.getBeanNamesForType()` and `DefaultListableBeanFactory.candidateNamesForType()`.

**After:**
```java
// Consolidated in BeanTypeMatchingService
public class BeanTypeMatchingService {
    public Set<String> findCandidateNamesForType(Class<?> type) { ... }
    public String choosePrimary(Class<?> requiredType, ...) { ... }
    public Set<String> getBeanNamesForType(Class<?> type) { ... }
}
```

**Benefits:**
- Centralized type matching logic.
- Eliminated duplication between BeanGraph and BeanFactory.
- Enables caching strategies.

### Improvement Results

#### Quantitative Improvements
- **SproutApplicationContext.refresh()**: Reduced from 19 lines to 10 lines (47% reduction).
- **DefaultListableBeanFactory**: Reduced from 357 lines to 280 lines (22% reduction).
- **Responsibility Separation**: From 1 class to 15 classes (Single Responsibility Principle).

#### Qualitative Improvements
- ✅ Clearer responsibilities for each component.
- ✅ Easier addition of new features (OCP compliance).
- ✅ Improved testability.
- ✅ Enhanced code readability and maintainability.
- ✅ Extensibility comparable to Spring.

### Backward Compatibility

**100% preservation of existing behavior:**
- Prior registration of infrastructure beans.
- Timely registration of BeanPostProcessors.
- Passing package information to PostInfrastructureInitializer.
- Post-processing of List injections.
- Dependency order guaranteed by topological sorting.
- Circular dependency detection.

### Future Extension Directions

The refactored architecture enables easy addition of:
1. **New Dependency Types**:
    - `Optional<T>`: Optional dependencies.
    - `Provider<T>`: Lazy loading.
    - `Map<String, T>`: Name-based bean mapping.

2. **New Bean Creation Methods**:
    - Builder pattern-based creation.
    - Static factory methods.
    - Prototype scope.

3. **New Lifecycle Phases**:
    - Event-driven extensibility.
    - Lazy initialization support.
    - Bean creation performance monitoring.

## Conclusion

Sprout’s IoC container is designed to be simpler and more predictable than Spring’s while maintaining similar functionality. The v2.0 refactoring applied the **Strategy Pattern, Chain of Responsibility Pattern, and Phase Pattern** to significantly improve extensibility and maintainability. By supporting only constructor injection and providing a clear bean lifecycle, it ensures ease of debugging and understanding.
