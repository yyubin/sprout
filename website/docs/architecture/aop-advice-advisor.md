# 🎯 AOP Advice & Advisor

## Overview

This document provides an in-depth technical analysis of the core components of the Sprout Framework's AOP (Aspect-Oriented Programming) system: **Advice** and **Advisor**. We will delve into the creation and storage of advice, pointcut matching strategies, and the internal structure of the advisor registry to foster a clear understanding of Sprout AOP's design philosophy and implementation mechanisms.

## AOP Architecture Overview

### Core Component Relationship Diagram

```
@Aspect Class
    ↓
AdviceFactory → AdviceBuilder → Advice (Interceptor)
    ↓                ↓              ↓
DefaultAdvisor ← Pointcut ← PointcutFactory
    ↓
AdvisorRegistry (Storage & Matching)
    ↓
Used during Proxy Creation
```

### Roles of Main Components

- **Advice**: The interceptor that executes the actual cross-cutting concern (the additional functionality).
- **Advisor**: A container that holds the **Advice**, **Pointcut**, and execution order.
- **Pointcut**: A condition that determines the join points where the advice should be applied.
- **AdviceFactory**: Analyzes annotations to create the appropriate Advisor.
- **AdvisorRegistry**: Stores the created Advisors and finds applicable Advisors for a given method.

-----

## Advice System Analysis

### 1\. Advice Interface: A Unified Interception Model

**A Simple and Powerful Interface**

```java
public interface Advice {
    Object invoke(MethodInvocation invocation) throws Throwable;
}
```

**Design Features**

1.  **Single Method**: All advice types use the same method signature, promoting simplicity.
2.  **`MethodInvocation` Based**: Similar to the Interceptor pattern in Spring.
3.  **Exception Transparency**: Allows propagation of all exceptions via `Throwable`.
4.  **Chaining Support**: Enables calling the next advice in the chain through `invocation.proceed()`.

### 2\. AdviceType: An Advice Classification System

**Enum-Based Type Management**

```java
public enum AdviceType {
    AROUND(Around.class),
    BEFORE(Before.class),
    AFTER(After.class);

    private final Class<? extends Annotation> anno;

    public static Optional<AdviceType> from(Method m) {
        return Arrays.stream(values())
                .filter(t -> m.isAnnotationPresent(t.anno))
                .findFirst();
    }
}
```

**Key Design Decisions**

1.  **Annotation-to-Type Mapping**: Each `AdviceType` holds its corresponding annotation class.
2.  **Stream-Based Search**: Uses the Java 8+ Stream API for concise type detection.
3.  **`Optional` Return**: Ensures null safety.
4.  **Extensibility**: Makes it easy to add new advice types.

### 3\. AdviceFactory: Centralized Advice Creation

**Combining the Factory and Strategy Patterns**

```java
@Component
public class AdviceFactory implements InfrastructureBean {
    private final Map<AdviceType, AdviceBuilder> builders;
    private final PointcutFactory pointcutFactory;

    public AdviceFactory(PointcutFactory pointcutFactory) {
        this.pointcutFactory = pointcutFactory;
        this.builders = Map.of(
            AdviceType.AROUND, new AroundAdviceBuilder(),
            AdviceType.BEFORE, new BeforeAdviceBuilder(),
            AdviceType.AFTER,  new AfterAdviceBuilder()
        );
    }

    public Optional<Advisor> createAdvisor(Class<?> aspectCls, Method m, Supplier<Object> sup) {
        return AdviceType.from(m)
                .map(type -> builders.get(type).build(aspectCls, m, sup, pointcutFactory));
    }
}
```

**Architectural Features**

1.  **Immutable Builder Map**: Uses `Map.of()` for compile-time mapping of builders.
2.  **Dependency Injection**: Receives `PointcutFactory` via constructor injection.
3.  **Type Safety**: Ensures type safety through generics and `Optional`.
4.  **Single Responsibility**: Solely responsible for creating advice, delegating the actual implementation to builders.

### 4\. AdviceBuilder Implementations

#### BeforeAdviceBuilder: Pre-processing Advice

**Parameter Validation and Builder Creation**

```java
public class BeforeAdviceBuilder implements AdviceBuilder {
    @Override
    public Advisor build(Class<?> aspectCls, Method method, Supplier<Object> aspectSup, PointcutFactory pf) {
        Before before = method.getAnnotation(Before.class);

        // 1. Validate parameters
        if (method.getParameterCount() > 1 ||
            (method.getParameterCount() == 1 &&
             !JoinPoint.class.isAssignableFrom(method.getParameterTypes()[0]))) {
            throw new IllegalStateException("@Before method must have 0 or 1 JoinPoint param");
        }

        // 2. Create Pointcut
        Pointcut pc = pf.createPointcut(before.annotation(), before.pointcut());

        // 3. Handle static methods
        Supplier<Object> safe = Modifier.isStatic(method.getModifiers()) ? () -> null : aspectSup;

        // 4. Create Advice and Advisor
        Advice advice = new SimpleBeforeInterceptor(safe, method);
        return new DefaultAdvisor(pc, advice, 0);
    }
}
```

#### AroundAdviceBuilder: Full-Control Advice

**Mandatory `ProceedingJoinPoint` Validation**

```java
public class AroundAdviceBuilder implements AdviceBuilder {
    @Override
    public Advisor build(Class<?> aspectCls, Method method, Supplier<Object> sup, PointcutFactory pf) {
        Around around = method.getAnnotation(Around.class);

        // Mandate ProceedingJoinPoint
        if (method.getParameterCount() != 1 ||
            !ProceedingJoinPoint.class.isAssignableFrom(method.getParameterTypes()[0])) {
            throw new IllegalStateException("Around advice method must have exactly one parameter of type ProceedingJoinPoint");
        }

        Pointcut pc = pf.createPointcut(around.annotation(), around.pointcut());
        Supplier<Object> safe = Modifier.isStatic(method.getModifiers()) ? () -> null : sup;

        Advice advice = new SimpleAroundInterceptor(safe, method);
        return new DefaultAdvisor(pc, advice, 0);
    }
}
```

**Characteristics of Around Advice**

1.  **Strict Signature**: Allows exactly one parameter of type `ProceedingJoinPoint`.
2.  **Complete Control**: The advice decides whether and when to invoke the original method.
3.  **Return Value Manipulation**: Can intercept and modify the original method's return value.

### 5\. Advice Interceptor Implementations

#### SimpleBeforeInterceptor: Pre-Execution Interceptor

**Invokes Original Method After Pre-Execution**

```java
public class SimpleBeforeInterceptor implements Advice {
    private final Supplier<Object> aspectProvider;
    private final Method adviceMethod;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // 1. Get the aspect instance (null if static)
        Object aspect = java.lang.reflect.Modifier.isStatic(adviceMethod.getModifiers())
                ? null : aspectProvider.get();

        try {
            // 2. Execute the advice method
            if (adviceMethod.getParameterCount() == 0) {
                adviceMethod.invoke(aspect);
            } else {
                JoinPoint jp = new JoinPointAdapter(invocation);
                adviceMethod.invoke(aspect, jp);
            }
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }

        // 3. Proceed to the original method
        return invocation.proceed();
    }
}
```

#### SimpleAfterInterceptor: Post-Execution Interceptor

**Handles `After` Logic Considering Exceptions**

```java
public class SimpleAfterInterceptor implements Advice {
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object result;
        Throwable thrown = null;

        try {
            // 1. Execute the original method first
            result = invocation.proceed();
        } catch (Throwable t) {
            thrown = t;
            result = null;
        }

        // 2. Execute After advice (regardless of exception)
        Object aspect = java.lang.reflect.Modifier.isStatic(adviceMethod.getModifiers())
                ? null : aspectProvider.get();

        try {
            if (adviceMethod.getParameterCount() == 0) {
                adviceMethod.invoke(aspect);
            } else {
                JoinPoint jp = new JoinPointAdapter(invocation);
                adviceMethod.invoke(aspect, jp);
            }
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }

        // 3. Re-throw the original exception if it exists
        if (thrown != null) throw thrown;
        return result;
    }
}
```

**Key Features of After Advice**

1.  **Exception-Agnostic Execution**: Uses a try-catch block to ensure advice runs even if an exception is caught.
2.  **Exception Preservation**: Re-propagates the original method's exception after the advice executes.
3.  **`finally` Semantics**: Behaves similarly to a Java `finally` block.

#### SimpleAroundInterceptor: The Full-Control Interceptor

**Complete Control via `ProceedingJoinPoint`**

```java
public class SimpleAroundInterceptor implements Advice {
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // 1. Create a ProceedingJoinPoint adapter
        ProceedingJoinPoint pjp = new PjpAdapter(invocation);

        // 2. Get the aspect instance
        Object aspect = java.lang.reflect.Modifier.isStatic(adviceMethod.getModifiers())
                ? null : aspectProvider.get();

        try {
            // 3. Execute the Around advice method, passing control of the original method invocation
            adviceMethod.setAccessible(true);
            return adviceMethod.invoke(aspect, pjp);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
}
```

**The Power of Around**

1.  **Invocation Control**: The advice decides whether to call `pjp.proceed()`.
2.  **Return Value Control**: Can intercept and alter the original method's return value.
3.  **Exception Handling**: Can handle exceptions from the original method with a try-catch block.

-----

## Advisor System Analysis

### 1\. Advisor Interface: Combining Advice and Pointcut

**A Simple and Clear Contract**

```java
public interface Advisor {
    Pointcut getPointcut();
    Advice getAdvice();
    default int getOrder() {
        return Integer.MAX_VALUE; // Default, lowest precedence
    }
}
```

**Design Philosophy**

1.  **Composition Pattern**: Combines `Advice` and `Pointcut` to form a complete advising unit.
2.  **Order Support**: `getOrder()` controls the execution order of multiple advices.
3.  **Default Value**: Provides the lowest precedence if an order is not specified.

### 2\. DefaultAdvisor: The Standard Advisor Implementation

**An Advisor Designed as an Immutable Object**

```java
public class DefaultAdvisor implements Advisor {
    private final Pointcut pointcut;
    private final Advice advice;
    private final int order;

    public DefaultAdvisor(Pointcut pointcut, Advice advice, int order) {
        this.pointcut = pointcut;
        this.advice = advice;
        this.order = order;
    }

    @Override
    public Pointcut getPointcut() { return pointcut; }

    @Override
    public Advice getAdvice() { return advice; }

    @Override
    public int getOrder() { return order; }
}
```

**Benefits of Immutability**

1.  **Thread Safety**: Safe in multi-threaded environments as its state cannot change after creation.
2.  **Predictability**: The behavior of a created advisor remains consistent.
3.  **Cache-Friendly**: Well-suited for caching strategies since its state is constant.

### 3\. AdvisorRegistry: The Advisor Store and Matcher

**A Concurrency-Aware Registry Design**

```java
@Component
public class AdvisorRegistry implements InfrastructureBean {
    private final List<Advisor> advisors = new ArrayList<>();
    private final Map<Method, List<Advisor>> cachedAdvisors = new ConcurrentHashMap<>();

    public void registerAdvisor(Advisor advisor) {
        synchronized (this) {
            advisors.add(advisor);
            cachedAdvisors.clear();  // Invalidate cache
            advisors.sort(Comparator.comparingInt(Advisor::getOrder));  // Sort by order
        }
    }

    public List<Advisor> getApplicableAdvisors(Class<?> targetClass, Method method) {
        List<Advisor> cached = cachedAdvisors.get(method);

        if (cached != null) {
            return cached;  // Cache hit
        }

        // Find applicable advisors
        List<Advisor> applicableAdvisors = new ArrayList<>();
        for (Advisor advisor : advisors) {
            if (advisor.getPointcut().matches(targetClass, method)) {
                applicableAdvisors.add(advisor);
            }
        }

        cachedAdvisors.put(method, applicableAdvisors);  // Cache the result
        return applicableAdvisors;
    }
}
```

**Key Optimization Strategies**

1.  **Per-Method Caching**: Caches applicable advisors for each method using a `ConcurrentHashMap`.
2.  **Pre-Sorting**: Sorts advisors by `order` at registration time to avoid sorting costs at runtime.
3.  **Cache Invalidation**: Clears the entire cache when a new advisor is registered.
4.  **Minimized Synchronization**: Uses `synchronized` for writes (registration) and `ConcurrentHashMap` for reads (lookups).

### 4\. Pointcut System

#### Pointcut Interface

**A Simple and Powerful Matching Interface**

```java
public interface Pointcut {
    boolean matches(Class<?> targetClass, Method method);
}
```

#### AnnotationPointcut: Annotation-Based Matching

**Hierarchical Annotation Search**

```java
public class AnnotationPointcut implements Pointcut {
    private final Class<? extends Annotation> annotationType;

    @Override
    public boolean matches(Class<?> targetClass, Method method) {
        // 1. Annotation directly on the method
        if (has(method)) return true;

        // 2. Annotation at the class level (declaring class and actual target class)
        if (has(method.getDeclaringClass()) || has(targetClass)) return true;

        return false;
    }

    private boolean has(AnnotatedElement el) {
        return el.isAnnotationPresent(annotationType);
    }
}
```

**Matching Precedence**

1.  **Method Level**: Annotations directly on the method have the highest priority.
2.  **Class Level**: Annotations on the method's declaring class and the actual target class are checked next.

#### AspectJPointcutAdapter: Support for AspectJ Expressions

**Integration with the AspectJ Library**

```java
public final class AspectJPointcutAdapter implements Pointcut {
    private static final PointcutParser PARSER =
        PointcutParser.getPointcutParserSupportingAllPrimitivesAndUsingContextClassloaderForResolution();

    private final PointcutExpression expression;

    public AspectJPointcutAdapter(String expr) {
        this.expression = PARSER.parsePointcutExpression(expr);
    }

    @Override
    public boolean matches(Class<?> targetClass, Method method) {
        // 1. Pre-filter at the class level
        if (!expression.couldMatchJoinPointsInType(targetClass)) {
            return false;
        }

        // 2. Match method execution join point
        var sm = expression.matchesMethodExecution(method);
        return sm.alwaysMatches() || sm.maybeMatches();
    }
}
```

**Benefits of AspectJ Integration**

1.  **Powerful Expressions**: Supports the rich set of AspectJ pointcut expressions.
2.  **Performance Optimization**: Avoids unnecessary method checks through class-level pre-filtering.
3.  **Standard Compliance**: Fully supports the standard AspectJ syntax.

#### CompositePointcut: The OR-Combination Pointcut

**Logical OR of Multiple Pointcuts**

```java
public class CompositePointcut implements Pointcut {
    private final List<Pointcut> pointcuts;

    @Override
    public boolean matches(Class<?> targetClass, Method method) {
        for (Pointcut pointcut : pointcuts) {
            if (pointcut.matches(targetClass, method)) {
                return true;  // True if any one matches
            }
        }
        return false;
    }
}
```

### 5\. PointcutFactory: Pointcut Creation Strategy

**Creating Pointcuts for Complex Conditions**

```java
@Component
public class DefaultPointcutFactory implements PointcutFactory, InfrastructureBean {

    @Override
    public Pointcut createPointcut(Class<? extends Annotation>[] annotationTypes, String aspectjExpr) {
        List<Pointcut> pcs = new ArrayList<>();

        // 1. Add annotation conditions
        if (annotationTypes != null && annotationTypes.length > 0) {
            for (Class<? extends Annotation> anno : annotationTypes) {
                pcs.add(new AnnotationPointcut(anno));
            }
        }

        // 2. Add AspectJ expression
        if (aspectjExpr != null && !aspectjExpr.isBlank()) {
            pcs.add(new AspectJPointcutAdapter(aspectjExpr.trim()));
        }

        // 3. Exception if no conditions
        if (pcs.isEmpty()) {
            throw new IllegalArgumentException("At least one of annotation[] or pointcut() must be provided.");
        }

        // 4. Return directly for a single condition, or combine with CompositePointcut for multiple
        return pcs.size() == 1 ? pcs.get(0) : new CompositePointcut(pcs);
    }
}
```

**Factory Flexibility**

1.  **Multiple Annotations**: Combines multiple annotation types with an OR condition.
2.  **AspectJ Support**: Handles complex pointcut expressions.
3.  **Composition Strategy**: Creates the optimal `Pointcut` based on the number of conditions.
4.  **Input Validation**: Ensures at least one condition is always provided.

-----

## Initialization and Lifecycle

### Advice Creation Process

1.  **Scan for `@Aspect` Classes**: Discovers aspect beans through component scanning.
2.  **Analyze Methods**: Detects `@Before`, `@After`, and `@Around` annotations on each method.
3.  **Determine `AdviceType`**: Selects the appropriate `AdviceType` based on the annotation.
4.  **Select `AdviceBuilder`**: Uses the corresponding builder to create an `Advisor`.
5.  **Register with `AdvisorRegistry`**: Registers the created `Advisor` in the central registry.

### Usage During Proxy Creation

1.  **Analyze Target Class**: Analyzes the class and methods of the proxy target.
2.  **Find Applicable Advisors**: Queries the `AdvisorRegistry` for matching Advisors.
3.  **Build Interceptor Chain**: Constructs an interceptor chain from the `Advice` of the matched Advisors.
4.  **Sort Interceptors**: Determines the execution order of interceptors based on their `Order` value.

-----

## Performance Analysis

### Time Complexity

**`AdvisorRegistry` Operations**

- **Advisor Registration**: O(n log n) (includes sorting)
- **Find Applicable Advisors**:
    - Cache Hit: O(1)
    - Cache Miss: O(n) (n = number of registered advisors)

**`PointcutMatcher` Operations**

- **`AnnotationPointcut`**: O(1) (annotation presence check)
- **`AspectJPointcutAdapter`**: O(1) (due to AspectJ's internal optimizations)
- **`CompositePointcut`**: O(m) (m = number of combined pointcuts)

### Memory Usage Optimization

**Caching Strategy**

```java
// Caching advisors per method reduces the cost of repeated lookups
private final Map<Method, List<Advisor>> cachedAdvisors = new ConcurrentHashMap<>();
```

**Use of Immutable Objects**

- **`DefaultAdvisor`**: Immutable for safe sharing across threads.
- **`AdviceType`**: Enum ensures a singleton pattern.
- **`Pointcut` Implementations**: Stateless matchers are reusable.

-----

## Comparison with Spring AOP

### Architectural Differences

| Feature | Spring AOP | Sprout AOP |
| :--- | :--- | :--- |
| **Advice Interface** | Different interfaces per type | Unified `Advice` interface |
| **Pointcut Support** | Various `Pointcut` types | Annotation + AspectJ |
| **Advisor Registration**| `BeanPostProcessor` | Explicit Registry |
| **Caching Strategy** | ProxyFactory level | Method level caching |
| **Interceptor Chain** | `ReflectiveMethodInvocation` | Custom `MethodInvocation` |

### Design Philosophy Differences

**Spring AOP**

- Provides dedicated interfaces for various advice types.
- Features complex `ProxyFactory` and `AdvisorChainFactory` components.

**Sprout AOP**

- Simplifies the model with a single `Advice` interface.
- Employs an explicit registry and factory patterns for clarity.

-----

## Extensibility and Customization

### Adding a New Advice Type

```java
// 1. Define a new advice type
public enum AdviceType {
    // ... existing types
    AFTER_RETURNING(AfterReturning.class),  // New addition
}

// 2. Implement a dedicated builder
public class AfterReturningAdviceBuilder implements AdviceBuilder {
    @Override
    public Advisor build(Class<?> aspectCls, Method method,
                         Supplier<Object> aspectSup, PointcutFactory pf) {
        // Implementation logic
    }
}

// 3. Register the builder in AdviceFactory
this.builders = Map.of(
    // ... existing builders
    AdviceType.AFTER_RETURNING, new AfterReturningAdviceBuilder()
);
```

### Implementing a Custom Pointcut

```java
public class CustomPointcut implements Pointcut {
    @Override
    public boolean matches(Class<?> targetClass, Method method) {
        // Custom matching logic
        return /* condition */;
    }
}
```

-----

Sprout's AOP Advice and Advisor system is designed to simplify the core concepts of Spring AOP for educational purposes, providing a structure that clearly demonstrates the operational principles of AOP. Through its unified `Advice` interface, explicit registry pattern, and efficient caching strategy, it delivers an implementation that balances both performance and readability.

Contributions and suggestions for improvement are always welcome\!