# 🚀 Request Dispatching System

## Overview

This document provides an in-depth technical analysis of the Sprout Framework's request dispatching system. It examines the entire processing pipeline, from the moment an HTTP request is parsed to the execution of a controller method and the generation of a response. We will detail the operational mechanisms of Filters and Interceptors, highlight differences with the Spring Framework, and analyze Sprout's unique design decisions.

## Dispatching Pipeline Architecture

### Overall Request Handling Flow

```

HttpRequest → RequestDispatcher → Controller Method → HttpResponse
↓
┌─────────────────────────────────────────────────────────────────────┐
│ DispatchHook → FilterChain → InterceptorChain → HandlerInvoker      │
│                                       ↓                             │
│ ResponseResolver ← ResponseAdvice ← Return Value                    │
└─────────────────────────────────────────────────────────────────────┘

````

### Step-by-Step Execution Order

**Request Phase**:
1.  `DispatchHook.beforeDispatch()` - Pre-processing hook
2.  `FilterChain.doFilter()` - Filter chain execution
3.  `InterceptorChain.applyPreHandle()` - Interceptor pre-processing
4.  `HandlerMethodInvoker.invoke()` - Controller method execution
5.  `InterceptorChain.applyPostHandle()` - Interceptor post-processing

**Response Phase**:
1.  `ResponseAdvice.beforeBodyWrite()` - Response advice processing
2.  `ResponseResolver.resolve()` - Response resolution
3.  `InterceptorChain.applyAfterCompletion()` - Interceptor completion processing
4.  `DispatchHook.afterDispatch()` - Post-processing hook

---

## Core Component Analysis

### 1. RequestDispatcher: The Central Coordinator

**Dependency Injection Structure**

```java
@Component
public class RequestDispatcher {
    private final HandlerMapping mapping;
    private final HandlerMethodInvoker invoker;
    private final List<ResponseResolver> responseResolvers;
    private final List<ResponseAdvice> responseAdvices;
    private final List<Filter> filters;
    private final List<Interceptor> interceptors;
    private final List<ExceptionResolver> exceptionResolvers;
    private final List<DispatchHook> dispatchHooks;
}
````

**Design Principles**

1.  **Dependency Inversion**: Depends on interfaces, not concrete implementations.
2.  **Composition Pattern**: Composes complex logic by combining multiple strategy objects.
3.  **Single Responsibility**: Focuses solely on coordinating the dispatching process, delegating actual processing to dedicated components.
4.  **Extensibility**: Supports multiple implementations for each component through `List`-based injection.

### 2\. Main Dispatch Logic Analysis

**`dispatch()` Method: The Top-Level Entry Point**

```java
public void dispatch(HttpRequest<?> req, HttpResponse res) throws IOException {
    try {
        // 1. Execute pre-processing hooks
        for (DispatchHook hook : dispatchHooks) {
            hook.beforeDispatch(req, res);
        }

        // 2. Connect the filter chain with the actual dispatch logic
        new FilterChain(filters, this::doDispatch).doFilter(req, res);
    } finally {
        // 3. Execute post-processing hooks (always runs)
        for (DispatchHook hook : dispatchHooks) {
            hook.afterDispatch(req, res);
        }
    }
}
```

**Key Design Features**

1.  **try-finally Pattern**: Guarantees the execution of post-processing hooks, regardless of whether an exception occurs.
2.  **Functional Interface Usage**: Passes a method reference with `this::doDispatch`.
3.  **Layered Execution**: Follows a strict order: Hooks → Filters → Actual Dispatch.

**`doDispatch()` Method: Core Business Logic**

```java
private void doDispatch(HttpRequest<?> req, HttpResponse res) {
    HandlerMethod hm = null;
    Exception caughtException = null;
    InterceptorChain interceptorChain = new InterceptorChain(interceptors);

    try {
        // 1. Handler Mapping
        hm = mapping.findHandler(req.getPath(), req.getMethod());
        if (hm == null) {
            // Directly create a 404 response
            res.setResponseEntity(
                new ResponseEntity<>("Not Found", null, ResponseCode.NOT_FOUND)
            );
            return;
        }

        // 2. Interceptor pre-processing
        if (!interceptorChain.applyPreHandle(req, res, hm)) {
            return; // Request handling stopped by an interceptor
        }

        // 3. Execute the controller method
        Object returnValue = invoker.invoke(hm.requestMappingInfo(), req);

        // 4. Interceptor post-processing
        interceptorChain.applyPostHandle(req, res, hm, returnValue);

        // 5. Response handling
        setResponseResolvers(returnValue, req, res);

    } catch (Exception e) {
        caughtException = e;
        // 6. Delegate exception handling
        handleException(req, res, hm, e);
    } finally {
        // 7. Interceptor completion processing
        if (hm != null) {
            interceptorChain.applyAfterCompletion(req, res, hm, caughtException);
        }
    }
}
```

### 3\. Exception Handling Strategy

**Layered Exception Resolution Mechanism**

```java
// Exception handling via the ExceptionResolver chain
Object handledReturnValue = null;
for (ExceptionResolver resolver : exceptionResolvers) {
    handledReturnValue = resolver.resolveException(req, res, hm, caughtException);
    if (handledReturnValue != null) {
        // If handled successfully, set the response
        if (handledReturnValue instanceof ResponseEntity) {
            res.setResponseEntity((ResponseEntity<?>) handledReturnValue);
        } else {
            setResponseResolvers(handledReturnValue, req, res);
        }
        return;
    }
}
```

**Exception Handling Design Features**

1.  **Chain of Responsibility**: Multiple resolvers attempt to handle the exception sequentially.
2.  **Early Exit**: The chain stops as soon as one resolver successfully handles the exception.
3.  **Flexible Return Value**: Can return a `ResponseEntity` directly or another object for further processing.
4.  **Type Safety**: Uses `instanceof` for runtime type checking.

### 4\. Response Handling Pipeline

**Collaboration between `ResponseResolver` and `ResponseAdvice`**

```java
private void setResponseResolvers(Object returnValue, HttpRequest<?> req, HttpResponse res) {
    if (res.isCommitted()) return; // Prevent duplicate processing

    // 1. Apply the ResponseAdvice chain
    Object processed = applyResponseAdvices(returnValue, req);

    // 2. Find and apply the appropriate ResponseResolver
    for (ResponseResolver resolver : responseResolvers) {
        if (resolver.supports(processed)) {
            ResponseEntity<?> responseEntity = resolver.resolve(processed, req);
            res.setResponseEntity(responseEntity);
            return;
        }
    }

    throw new IllegalStateException("No suitable ResponseResolver found");
}
```

**`ResponseAdvice` Chain Processing**

```java
private Object applyResponseAdvices(Object returnValue, HttpRequest<?> req) {
    Object current = returnValue;
    for (ResponseAdvice advice : responseAdvices) {
        if (advice.supports(current, req)) {
            current = advice.beforeBodyWrite(current, req);
        }
    }
    return current;
}
```

-----

## Filter System Analysis

### FilterChain Implementation

**Chain of Responsibility Pattern**

```java
public class FilterChain {
    private final List<Filter> filters;
    private final Dispatcher dispatcher;
    private int currentFilterIndex = 0;

    public void doFilter(HttpRequest<?> request, HttpResponse response) throws IOException {
        if (currentFilterIndex < filters.size()) {
            // Execute the next filter
            filters.get(currentFilterIndex++).doFilter(request, response, this);
            return;
        }
        // After all filters are done, invoke the actual dispatcher
        dispatcher.dispatch(request, response);
    }
}
```

**FilterChain Features**

1.  **State-Based Progression**: Tracks the current execution position with `currentFilterIndex`.
2.  **Recursive Invocation**: Each filter invokes the next element in the chain.
3.  **Functional Interface**: Delegates final processing via the `Dispatcher` functional interface.
4.  **Linear Execution**: Filters are executed sequentially.

### Filter Interface

```java
public interface Filter extends InfrastructureBean {
    void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) throws IOException;
}
```

**Sprout Filter vs. Servlet Filter**

| Feature | Sprout Filter | Servlet Filter |
| :--- | :--- | :--- |
| **Parameters** | `HttpRequest`, `HttpResponse`, `FilterChain` | `ServletRequest`, `ServletResponse`, `FilterChain` |
| **Checked Exceptions**| `IOException` | `IOException`, `ServletException` |
| **Lifecycle** | Spring Bean Lifecycle | Managed by Servlet Container |
| **Configuration** | `@Component` + DI | `web.xml` or `@WebFilter` |

### `CorsFilter`: A Practical Example

**Configuration-Based CORS Handling**

```java
@Component
public class CorsFilter implements Filter {
    private final AppConfig appConfig;

    @Override
    public void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) throws IOException {
        String origin = Optional.ofNullable(request.getHeaders().get("Origin"))
                .map(Object::toString)
                .orElse(null);

        if (origin == null || origin.isEmpty()) {
            chain.doFilter(request, response); // Skip CORS if no Origin header
            return;
        }

        // Set CORS headers
        applyCorsHeaders(response, origin);

        // Handle OPTIONS preflight requests
        if (request.getMethod().equals(HttpMethod.OPTIONS)) {
            handlePreflightRequest(request, response);
            return; // Stop chain progression
        }

        chain.doFilter(request, response); // Continue to the next in the chain
    }
}
```

-----

## Interceptor System Analysis

### InterceptorChain Implementation

**Sequential Execution and Reverse-Order Cleanup**

```java
public class InterceptorChain {
    private final List<Interceptor> interceptors;

    public boolean applyPreHandle(HttpRequest request, HttpResponse response, Object handler) {
        for (Interceptor interceptor : interceptors) {
            if (!interceptor.preHandle(request, response, handler)) {
                return false; // Stop if any interceptor returns false
            }
        }
        return true;
    }

    public void applyPostHandle(HttpRequest request, HttpResponse response, Object handler, Object result) {
        // Execute in reverse order (LIFO)
        for (int i = interceptors.size() - 1; i >= 0; i--) {
            interceptors.get(i).postHandle(request, response, handler, result);
        }
    }

    public void applyAfterCompletion(HttpRequest request, HttpResponse response, Object handler, Exception ex) {
        // Execute in reverse order (LIFO)
        for (int i = interceptors.size() - 1; i >= 0; i--) {
            interceptors.get(i).afterCompletion(request, response, handler, ex);
        }
    }
}
```

**Interceptor Execution Pattern**

1.  **`preHandle`**: Executes in forward order. If any returns `false`, the entire chain (including the controller) is aborted.
2.  **`postHandle`**: Executes in reverse (LIFO) order, but only if the controller method executes successfully.
3.  **`afterCompletion`**: Executes in reverse (LIFO) order, always, regardless of whether an exception occurred.

### Interceptor vs. Filter Comparison

| Feature | Interceptor | Filter |
| :--- | :--- | :--- |
| **Execution Point** | After handler mapping | Before handler mapping |
| **Accessible Info** | Can access handler info | Cannot access handler info |
| **Execution Phases** | 3 phases (pre/post/after) | 1 phase (doFilter) |
| **Processing Scope** | Centered on controller methods| The entire HTTP request |
| **Execution Order** | Post-processing is LIFO | Always sequential |
| **Interruption** | `boolean` return value | Not calling the chain |

-----

## HandlerMethodInvoker Analysis

### Method Execution Strategy

```java
@Component
public class HandlerMethodInvoker {
    private final CompositeArgumentResolver resolvers;

    public Object invoke(RequestMappingInfo requestMappingInfo, HttpRequest<?> request) throws Exception {
        PathPattern pattern = requestMappingInfo.pattern();

        // 1. Extract path variables from the URL pattern
        Map<String, String> pathVariables = pattern.extractPathVariables(request.getPath());

        // 2. Resolve controller method parameters
        Object[] args = resolvers.resolveArguments(requestMappingInfo.handlerMethod(), request, pathVariables);

        // 3. Invoke the method via reflection
        return requestMappingInfo.handlerMethod().invoke(requestMappingInfo.controller(), args);
    }
}
```

**Core Processing Steps**

1.  **Path Variable Extraction**: Uses `PathPattern` to extract variables like `{id}` from the URL.
2.  **Argument Resolution**: Employs `CompositeArgumentResolver` to determine values for method parameters.
3.  **Method Invocation**: Executes the actual controller method using the Java Reflection API.

-----

## ControllerAdvice-Based Exception Handling System

Sprout provides an exception handling system that mimics Spring's `@ControllerAdvice` and `@ExceptionHandler` annotations.

### ControllerAdviceRegistry: The Exception Handler Registry

**Scanning and Registering Exception Handlers**

```java
@Component
public class ControllerAdviceRegistry {
    private final List<ExceptionHandlerObject> allExceptionHandlers = new ArrayList<>();
    private final Map<Class<? extends Throwable>, Optional<ExceptionHandlerObject>> cachedHandlers = new ConcurrentHashMap<>();

    public void scanControllerAdvices(BeanFactory context) {
        Collection<Object> allBeans = context.getAllBeans();
        for (Object bean : allBeans) {
            if (bean.getClass().isAnnotationPresent(ControllerAdvice.class)) {
                // Found a @ControllerAdvice class
                for (Method method : bean.getClass().getDeclaredMethods()) {
                    if (method.isAnnotationPresent(ExceptionHandler.class)) {
                        method.setAccessible(true);
                        allExceptionHandlers.add(new ExceptionHandlerObject(method, bean));
                    }
                }
            }
        }
    }
}
```

**Key Design Features**

1.  **Reflection-Based Scanning**: Discovers `@ControllerAdvice` beans at runtime.
2.  **Method Accessibility**: `setAccessible(true)` allows invocation of private handler methods.
3.  **Caching Mechanism**: Caches handlers by exception type using `ConcurrentHashMap`.

### Exception Handler Matching Algorithm

**Best-Match Handler Selection Strategy**

```java
private Optional<ExceptionHandlerObject> lookupBestMatchHandler(Class<? extends Throwable> exceptionClass) {
    ExceptionHandlerObject bestMatch = null;
    int bestMatchDistance = Integer.MAX_VALUE;

    for (ExceptionHandlerObject handler : allExceptionHandlers) {
        Method handlerMethod = handler.getMethod();
        for (Class<? extends Throwable> handledExceptionType : handlerMethod.getAnnotation(ExceptionHandler.class).value()) {
            if (handledExceptionType.isAssignableFrom(exceptionClass)) {
                // Calculate distance in the exception hierarchy
                int distance = getExceptionDistance(handledExceptionType, exceptionClass);

                if (distance < bestMatchDistance) {
                    bestMatch = handler;
                    bestMatchDistance = distance;
                }
            }
        }
    }
    return Optional.ofNullable(bestMatch);
}

private int getExceptionDistance(Class<?> fromClass, Class<?> toClass) {
    if (fromClass.equals(toClass)) return 0; // Exact type match

    int distance = 0;
    Class<?> current = toClass;
    while (current != null && !current.equals(fromClass)) {
        current = current.getSuperclass();
        distance++;
    }
    return (current != null) ? distance : Integer.MAX_VALUE;
}
```

**Matching Algorithm Features**

1.  **Distance-Based Matching**: Selects the closest (most specific) handler in the exception type hierarchy.
2.  **Multi-Exception Support**: `@ExceptionHandler({Exception1.class, Exception2.class})`.
3.  **Inheritance Awareness**: A parent exception handler can process child exceptions.
4.  **Exact Match Priority**: An identical type match has a distance of 0 and is prioritized.

### ControllerAdviceExceptionResolver Implementation

**Exception Resolution and Method Invocation**

```java
@Component
@Order(0) // Highest priority
public class ControllerAdviceExceptionResolver implements ExceptionResolver {

    @Override
    public Object resolveException(HttpRequest<?> request, HttpResponse response,
                                     Object handlerMethod, Exception exception) {
        Optional<ExceptionHandlerObject> handlerOptional =
            controllerAdviceRegistry.getExceptionHandler(exception.getClass());

        if (handlerOptional.isPresent()) {
            ExceptionHandlerObject exceptionHandler = handlerOptional.get();
            Method handlerMethodRef = exceptionHandler.getMethod();
            Object handlerInstance = exceptionHandler.getBean();

            // Support for various method signatures
            Object handlerReturnValue = invokeExceptionHandler(handlerMethodRef, handlerInstance, exception, request);

            // Response processing via ResponseResolver
            return processHandlerReturnValue(handlerReturnValue, request, response);
        }
        return null; // Could not handle
    }
}
```

**Supported Method Signature Patterns**

```java
// 1. No parameters
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<String> handleIllegalArgument() { }

// 2. Exception only
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) { }

// 3. Exception + Request
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e, HttpRequest request) { }

// 4. Request + Exception (reversed order)
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<String> handleIllegalArgument(HttpRequest request, IllegalArgumentException e) { }
```

**Method Invocation Strategy**

```java
private Object invokeExceptionHandler(Method handlerMethodRef, Object handlerInstance,
                                        Exception exception, HttpRequest request) {
    int paramCount = handlerMethodRef.getParameterCount();
    Class<?>[] paramTypes = handlerMethodRef.getParameterTypes();

    if (paramCount == 0) {
        return handlerMethodRef.invoke(handlerInstance);
    } else if (paramCount == 1 && paramTypes[0].isAssignableFrom(exception.getClass())) {
        return handlerMethodRef.invoke(handlerInstance, exception);
    } else if (paramCount == 2) {
        // Determine by type, regardless of parameter order
        if (paramTypes[0].isAssignableFrom(exception.getClass()) &&
            paramTypes[1].isAssignableFrom(HttpRequest.class)) {
            return handlerMethodRef.invoke(handlerInstance, exception, request);
        } else if (paramTypes[0].isAssignableFrom(HttpRequest.class) &&
                   paramTypes[1].isAssignableFrom(exception.getClass())) {
            return handlerMethodRef.invoke(handlerInstance, request, exception);
        }
    }
    throw new UnsupportedOperationException("Unsupported method signature");
}
```

### ExceptionHandlerObject: Handler Metadata

**A Simple Metadata Holder**

```java
public class ExceptionHandlerObject {
    private final Method method;
    private final Object bean;

    public ExceptionHandlerObject(Method method, Object bean) {
        this.method = method;
        this.bean = bean;
        method.setAccessible(true); // Allow invocation of private methods
    }
}
```

### Annotation Definitions

**`@ControllerAdvice` Annotation**

```java
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ControllerAdvice {
    // Applicable only at the class level
}
```

**`@ExceptionHandler` Annotation**

```java
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ExceptionHandler {
    Class<? extends Throwable>[] value(); // Array of exception types to handle
}
```

### Overall Exception Handling Flow

**Integration within `RequestDispatcher`**

```java
// Exception handling block within the doDispatch() method
catch (Exception e) {
    caughtException = e;

    Object handledReturnValue = null;
    for (ExceptionResolver resolver : exceptionResolvers) {
        // ControllerAdviceExceptionResolver runs first due to @Order(0)
        handledReturnValue = resolver.resolveException(req, res, hm, caughtException);
        if (handledReturnValue != null) {
            // Handled by an @ExceptionHandler
            setResponseResolvers(handledReturnValue, req, res);
            return;
        }
    }
    // Default handling for unhandled exceptions
}
```

**Execution Order**

1.  **Exception is thrown** in a controller.
2.  **`ControllerAdviceExceptionResolver` executes** (highest priority).
3.  The resolver **searches for a handler** in `ControllerAdviceRegistry`.
4.  The **most specific handler** is selected.
5.  The **handler method is invoked**.
6.  The response is converted by a **`ResponseResolver`**.
7.  If not handled, control passes to the **next `ExceptionResolver`**.

### Comparison with Spring

**`@ControllerAdvice` Feature Comparison**

| Feature | Spring `@ControllerAdvice` | Sprout `@ControllerAdvice` |
| :--- | :--- | :--- |
| **Applicability** | Global or limited by package/class | Global only |
| **Annotation Target** | Class level | Class level |
| **Scanning** | Automatic via component scanning | Manual scan from `BeanFactory` |
| **Method Signature** | Very flexible (Model, WebRequest, etc.) | Limited (Exception, HttpRequest) |
| **Response Handling**| `ViewResolver`, `HttpMessageConverter` | `ResponseResolver` |
| **Caching** | Built-in | Manual (`ConcurrentHashMap`) |

**`@ExceptionHandler` Feature Comparison**

| Feature | Spring `@ExceptionHandler` | Sprout `@ExceptionHandler` |
| :--- | :--- | :--- |
| **Parameter Support** | 20+ types (Model, HttpSession, etc.) | 4 patterns only |
| **Return Value Support**| `ResponseEntity`, `Model`, `View`, etc. | `ResponseResolver` dependent |
| **Exception Matching**| Runtime resolution | Compile-time + Runtime |
| **Order Control** | Supports `@Order` | Distance-based algorithm |
| **Async Support** | `DeferredResult`, `Callable` | Not supported |

### Performance and Memory Characteristics

**Caching Strategy**

```java
// Caching handlers by exception type for performance optimization
private final Map<Class<? extends Throwable>, Optional<ExceptionHandlerObject>> cachedHandlers = new ConcurrentHashMap<>();

public Optional<ExceptionHandlerObject> getExceptionHandler(Class<? extends Throwable> exceptionClass) {
    return cachedHandlers.computeIfAbsent(exceptionClass, this::lookupBestMatchHandler);
}
```

**Time Complexity**

* **First Lookup**: O(n × m) (where n = \# of handlers, m = \# of exception types per handler)
* **Cache Hit**: O(1)
* **Exception Distance Calculation**: O(d) (where d = depth of inheritance hierarchy)

**Memory Usage**

* **Handler Storage**: One `ExceptionHandlerObject` instance per `@ExceptionHandler` method.
* **Cache Storage**: One `Optional<ExceptionHandlerObject>` per looked-up exception type.
* **Method Accessibility**: `setAccessible(true)` call saves security check overhead.

-----

## Comparative Analysis with Spring Framework

### Architectural Differences

**Spring `DispatcherServlet` vs. Sprout `RequestDispatcher`**

| Aspect | Spring `DispatcherServlet` | Sprout `RequestDispatcher` |
| :--- | :--- | :--- |
| **Base Technology** | Servlet API-based | Pure Java-based |
| **Lifecycle** | Managed by Servlet Container| Spring Bean Lifecycle |
| **Initialization** | `init()`, `destroy()` | Constructor Injection |
| **Exception Handling**| `HandlerExceptionResolver` | `ExceptionResolver` |
| **View Resolution** | `ViewResolver` | `ResponseResolver` |

### Filter System Differences

**Execution Point and Scope**

```java
// Spring: Servlet Container Level
public class SpringFilter implements Filter {
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        // Executes in the Servlet Container
        // Runs before the DispatcherServlet
    }
}

// Sprout: Application Level
public class SproutFilter implements Filter {
    public void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) {
        // Executed directly by the RequestDispatcher
        // Runs within the application context
    }
}
```

**Lifecycle Management**

* **Spring**: The servlet container manages the filter lifecycle; configured via `@WebFilter` or `web.xml`.
* **Sprout**: The Spring IoC container manages the lifecycle; automatically registered with `@Component`.

### Interceptor Implementation Differences

**`HandlerInterceptor` vs. `Interceptor`**

```java
// Spring HandlerInterceptor
public interface HandlerInterceptor {
    default boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) { return true; }
    default void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {}
    default void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {}
}

// Sprout Interceptor
public interface Interceptor extends InfrastructureBean {
    boolean preHandle(HttpRequest request, HttpResponse response, Object handler);
    void postHandle(HttpRequest request, HttpResponse response, Object handler, Object result);
    void afterCompletion(HttpRequest request, HttpResponse response, Object handler, Exception ex);
}
```

**Key Differences**

1.  **Parameter Types**: Servlet API vs. custom HTTP abstraction.
2.  **`ModelAndView`**: Spring passes a view model; Sprout passes the raw return value.
3.  **Default Implementation**: Spring uses `default` methods; Sprout requires all methods to be implemented.

-----

## Performance Analysis

### Time Complexity

**Complexity per Request Processing Stage**

* **DispatchHook Execution**: O(h) (h = number of hooks)
* **Filter Chain**: O(f) (f = number of filters)
* **Handler Mapping**: O(log n) (n = number of registered handlers, assuming tree-based lookup)
* **Interceptor Chain**: O(i) (i = number of interceptors)
* **Argument Resolution**: O(p) (p = number of method parameters)
* **Method Execution**: O(1) (reflection call)
* **Response Handling**: O(r + a) (r = number of resolvers, a = number of advices)

**Overall Time Complexity**: O(h + f + log n + i + p + r + a)

### Memory Usage Patterns

**Object Creation and GC Pressure**

```java
// Objects created on every request
InterceptorChain interceptorChain = new InterceptorChain(interceptors); // Chain object
Map<String, String> pathVariables = pattern.extractPathVariables(request.getPath()); // Path variable map
Object[] args = resolvers.resolveArguments(handlerMethod, request, pathVariables); // Argument array
```

**Optimization Strategies**

1.  **Chain Object Pooling**: Reuse `InterceptorChain` instances.
2.  **Argument Array Caching**: Reuse arrays for identical method signatures.
3.  **Path Variable Optimization**: Use a singleton for empty maps.

### Parallel Processing Considerations

**Thread Safety**

* **RequestDispatcher**: Stateless component, thread-safe.
* **FilterChain**: Holds state (`currentFilterIndex`), requires a new instance per request.
* **InterceptorChain**: Stateless, thread-safe.

**Concurrency Optimization**

```java
// Current: new chain per request
new FilterChain(filters, this::doDispatch).doFilter(req, res);

// Optimization: ThreadLocal-based chain reuse is possible
private final ThreadLocal<FilterChain> chainCache = ThreadLocal.withInitial(() -> new FilterChain(filters, this::doDispatch));
```

-----

## Extensibility and Maintainability

### Adding New Processing Steps

**Extension Points**

1.  **`DispatchHook`**: Add logic before/after the entire request cycle.
2.  **`Filter`**: Pre-process at the HTTP level (auth, logging, CORS).
3.  **`Interceptor`**: Pre/post-process at the controller level (permissions, logging).
4.  **`ExceptionResolver`**: Add new exception handling strategies.
5.  **`ResponseResolver`**: Support new response formats.

### Configuration and Auto-Configuration

**Bean Registration and Order Control**

```java
@Configuration
public class WebConfig {
    @Bean
    @Order(1) // Controls execution order
    public Filter corsFilter() {
        return new CorsFilter();
    }

    @Bean
    @Order(2)
    public Filter authenticationFilter() {
        return new AuthenticationFilter();
    }
}
```

### Testability

**Unit Testing Strategy**

```java
@Test
void testRequestDispatching() {
    // Given
    HttpRequest request = mockRequest("/api/users/123");
    HttpResponse response = mockResponse();
    HandlerMapping mapping = mockMapping();

    RequestDispatcher dispatcher = new RequestDispatcher(
        mapping, invoker, resolvers, advices,
        List.of(), List.of(), List.of(), List.of()
    );

    // When
    dispatcher.dispatch(request, response);

    // Then
    assertEquals(ResponseCode.OK, response.getStatusCode());
}
```

-----

## Security Considerations

### Current Security Mechanisms

**1. Handler Mapping Security**

* Returns a 404 response if no handler is found.
* Prevents direct exposure of internal exceptions.

**2. Exception Handling Security**

* Stack traces are printed to the console (inappropriate for production).
* `ExceptionResolver` enables safe transformation of exceptions.

### Security Improvement Suggestions

**1. Prevent Information Exposure**

```java
// Current: Exposes debugging info
System.err.println("Exception caught in doDispatch: " + e.getMessage());
e.printStackTrace();

// Improved: Control based on logging level
if (logger.isDebugEnabled()) {
    logger.debug("Exception in doDispatch", e);
} else {
    logger.error("Request processing failed: {}", request.getPath());
}
```

**2. Strengthen Input Validation**

```java
// Recommended: Limit request size
public void dispatch(HttpRequest<?> req, HttpResponse res) {
    validateRequest(req); // Validate size, header count, etc.
    // ... existing logic
}
```

**3. CSRF/XSS Prevention**

```java
@Component
public class SecurityFilter implements Filter {
    public void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) {
        applyCsrfProtection(request, response);
        applyXssProtection(response);
        chain.doFilter(request, response);
    }
}
```

-----

We welcome new opinions, suggestions for vulnerabilities and improvements, and pull requests\!

