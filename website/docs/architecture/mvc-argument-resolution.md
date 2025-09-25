# 🎁 MVC Argument Resolution

## Overview

This document provides a technical analysis of Sprout Framework's argument resolution system, which handles the automatic binding of HTTP request data to controller method parameters. The system transforms raw HTTP data (path variables, headers, query parameters, request body) into strongly-typed method arguments through a flexible, extensible resolver chain.

## Architecture Overview

### Argument Resolution Flow

```
HTTP Request Data → CompositeArgumentResolver → Specific ArgumentResolvers
                                ↓
Controller Method Parameters ← TypeConverter ← Resolved Arguments
```

### Component Interaction

```
┌─────────────────────┐    ┌──────────────────────┐    ┌─────────────────┐
│   HTTP Request      │───→│ CompositeArgument    │───→│ ArgumentResolver│
│   - Path Variables  │    │ Resolver             │    │ Implementations │
│   - Headers         │    │ (Coordinator)        │    │                 │
│   - Query Params    │    └──────────────────────┘    └─────────────────┘
│   - Body            │                ↓                         ↓
└─────────────────────┘    ┌──────────────────────┐    ┌─────────────────┐
                           │   Type Converter     │←───│ Resolved Values │
                           │   (Type Safety)      │    │                 │
                           └──────────────────────┘    └─────────────────┘
```

## Core Components Analysis

### 1. CompositeArgumentResolver: The Resolution Orchestrator

**Delegation Strategy**

The `CompositeArgumentResolver` implements the Composite pattern to coordinate multiple specialized resolvers.

```java
public Object[] resolveArguments(Method method, HttpRequest<?> request,
                                 Map<String, String> pathVariables) throws Exception {
    Parameter[] params = method.getParameters();
    Object[] args = new Object[params.length];

    for (int i = 0; i < params.length; i++) {
        Parameter p = params[i];

        // Find first supporting resolver
        ArgumentResolver resolver = delegates.stream()
                .filter(ar -> ar.supports(p))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No ArgumentResolver for parameter " + p));

        args[i] = resolver.resolve(p, request, pathVariables);
    }
    return args;
}
```

**Design Pattern Analysis**

1. **Chain of Responsibility**: Each resolver checks if it can handle a parameter
2. **Strategy Pattern**: Different resolution strategies for different parameter types
3. **Template Method**: Common resolution framework with specialized implementations

**Performance Characteristics**

- **Time Complexity**: O(n * m) where n = parameters, m = average resolvers checked
- **Early Termination**: Stops at first matching resolver
- **Caching Opportunity**: Could cache resolver mappings for repeated calls

**Error Handling Strategy**

```java
.orElseThrow(() -> new IllegalStateException("No ArgumentResolver for parameter " + p));
```

- Fail-fast approach: Unknown parameter types cause immediate failure
- Clear error messages for debugging
- No partial resolution attempts

### 2. ArgumentResolver Interface: The Resolver Contract

**Contract Definition**

```java
public interface ArgumentResolver {
    boolean supports(Parameter parameter);
    Object resolve(Parameter parameter, HttpRequest<?> request,
                   Map<String, String> pathVariables) throws Exception;
}
```

**Two-Phase Resolution Protocol**

1. **Support Check**: Determines if resolver can handle the parameter
2. **Resolution**: Performs actual value extraction and conversion

**Interface Design Benefits**

- **Extensibility**: Easy to add new resolver types
- **Testability**: Each resolver can be unit tested independently
- **Separation of Concerns**: Clear responsibility boundaries

### 3. TypeConverter: Centralized Type Conversion

**Conversion Algorithm**

```java
public static Object convert(String value, Class<?> targetType) {
    if (value == null) {
        if (targetType.isPrimitive()) {
            throw new IllegalArgumentException(
                    "Null value cannot be assigned to primitive type: " + targetType.getName());
        }
        return null;
    }

    if (targetType.equals(String.class)) {
        return value;
    } else if (targetType.equals(Long.class) || targetType.equals(long.class)) {
        return Long.parseLong(value);
    } else if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
        return Integer.parseInt(value);
    } else if (targetType.equals(Boolean.class) || targetType.equals(boolean.class)) {
        return Boolean.parseBoolean(value);
    }

    throw new IllegalArgumentException(
            "Cannot convert String value [" + value + "] to target class [" + targetType.getName() + "]");
}
```

**Type Safety Features**

1. **Primitive Null Check**: Prevents null assignment to primitive types
2. **Wrapper Type Handling**: Supports both primitive and wrapper types
3. **Explicit Type Mapping**: Clear conversion rules for supported types
4. **Fail-Safe**: Throws exception for unsupported conversions

**Supported Conversions**

| Source | Target Types | Conversion Method |
|--------|--------------|-------------------|
| String | String | Identity |
| String | Long/long | Long.parseLong() |
| String | Integer/int | Integer.parseInt() |
| String | Boolean/boolean | Boolean.parseBoolean() |

## Resolver Implementation Analysis

### 1. PathVariableArgumentResolver

**Support Detection**

```java
public boolean supports(Parameter parameter) {
    return parameter.isAnnotationPresent(PathVariable.class);
}
```

**Resolution Logic**

```java
public Object resolve(Parameter parameter, HttpRequest<?> request,
                      Map<String, String> pathVariables) throws Exception {
    PathVariable pathVariableAnnotation = parameter.getAnnotation(PathVariable.class);
    String variableName = pathVariableAnnotation.value();

    if (variableName.isEmpty()) {
        variableName = parameter.getName();  // Convention over configuration
    }

    String value = pathVariables.get(variableName);
    if (value == null) {
        throw new IllegalArgumentException(
                "Path variable '" + variableName + "' not found in path.");
    }

    return TypeConverter.convert(value, parameter.getType());
}
```

**Key Features**

- **Convention over Configuration**: Uses parameter name if annotation value is empty
- **Strict Validation**: Throws exception if path variable not found
- **Type Conversion**: Delegates to centralized converter

### 2. RequestParamArgumentResolver

**Enhanced Support Logic**

```java
public Object resolve(Parameter parameter, HttpRequest<?> request,
                      Map<String, String> pathVariables) throws Exception {
    RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
    String paramName = requestParam.value().isEmpty() ?
            parameter.getName() : requestParam.value();

    String paramValue = request.getQueryParams().get(paramName);

    if (paramValue == null) {
        if (requestParam.required()) {
            throw new IllegalArgumentException(
                    "Required request parameter '" + paramName + "' not found in request.");
        }
    }

    return TypeConverter.convert(paramValue, parameter.getType());
}
```

**Advanced Features**

- **Optional Parameters**: Supports required/optional distinction
- **Null Handling**: Graceful handling of missing optional parameters
- **Validation Logic**: Enforces required parameter constraints

### 3. HeaderArgumentResolver

**Dual-Mode Resolution**

The system provides two header resolvers.

**Individual Header Resolution**
```java
// HeaderArgumentResolver - handles specific headers
public boolean supports(Parameter parameter) {
    return parameter.isAnnotationPresent(Header.class) && 
           !parameter.getAnnotation(Header.class).value().isEmpty();
}
```

**All Headers Resolution**
```java
// AllHeaderArgumentResolver - handles header map injection
public boolean supports(Parameter parameter) {
    return parameter.isAnnotationPresent(Header.class) && 
           parameter.getAnnotation(Header.class).value().isEmpty();
}
```

**Type-Based Dispatch**

```java
if (parameter.getType().equals(Map.class)) {
    if (headerName.isBlank()) {
        return request.getHeaders();  // Return all headers
    } else {
        throw new IllegalArgumentException(
            "Cannot bind specific header '" + headerName + 
            "' to a Map parameter. Use Map<String, String> without @Header for all headers.");
    }
}
```

### 4. RequestBodyArgumentResolver

**JSON Deserialization Integration**

```java
@Component
public class RequestBodyArgumentResolver implements ArgumentResolver {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Object resolve(Parameter parameter, HttpRequest<?> request, 
                         Map<String, String> pathVariables) throws Exception {
        String rawBody = (String) request.getBody();

        if (rawBody == null || rawBody.isBlank()) {
            return null; 
        }

        try {
            return objectMapper.readValue(rawBody.trim(), parameter.getType());
        } catch (Exception e) {
            throw new BadRequestException(
                "Failed to parse request body as JSON or convert to '" + 
                parameter.getType().getName() + "'. Check JSON format and target type. Cause: " + 
                e.getMessage(), ResponseCode.BAD_REQUEST, e);
        }
    }
}
```

**Advanced Features**

- **Jackson Integration**: Uses ObjectMapper for JSON deserialization
- **Generic Type Support**: Can deserialize to any class
- **Error Context**: Provides detailed error information
- **HTTP Status Mapping**: Maps parsing errors to appropriate HTTP responses

## Type System Integration

### Java Reflection Integration

**Parameter Introspection**

```java
Parameter[] params = method.getParameters();
// Each Parameter provides:
// - parameter.getType() - Class<?> for type checking
// - parameter.getName() - String for convention over configuration
// - parameter.getAnnotations() - Annotation[] for metadata
// - parameter.isAnnotationPresent(Class) - boolean for quick checks
```

**Type Erasure Handling**

The system currently handles basic types but has limitations with generic types.

```java
// Works
public void method(@RequestBody User user)

// Limited - loses generic type information
public void method(@RequestBody List<User> users)
```

**Potential Enhancement**

```java
// Could use ParameterizedType for generic support
if (parameter.getParameterizedType() instanceof ParameterizedType) {
    ParameterizedType pType = (ParameterizedType) parameter.getParameterizedType();
    Type[] actualTypeArguments = pType.getActualTypeArguments();
    // Handle List<User>, Map<String, Object>, etc.
}
```

## Performance Analysis

### Resolution Complexity

**Per-Request Resolution**
- Time: O(p * r) where p = parameters, r = average resolvers to check
- Space: O(p) for argument array allocation
- Optimization: Could implement resolver caching

**Type Conversion Overhead**
- Primitive conversions: O(1)
- String operations: O(1)
- JSON deserialization: O(json_size)

### Memory Usage Patterns

**Resolver Chain**
```java
private final List<ArgumentResolver> delegates;
```
- Static resolver list shared across all requests
- No per-request resolver allocation

**Argument Array**
```java
Object[] args = new Object[params.length];
```
- Temporary array per method invocation
- Size determined by method signature

### Optimization Opportunities

**Resolver Mapping Cache**
```java
// Potential enhancement
private final Map<Parameter, ArgumentResolver> resolverCache = new ConcurrentHashMap<>();

public Object[] resolveArguments(...) {
    // Cache resolver mappings per parameter
    ArgumentResolver resolver = resolverCache.computeIfAbsent(p, 
        param -> delegates.stream().filter(ar -> ar.supports(param)).findFirst().orElse(null));
}
```

## Error Handling Strategy

### Exception Hierarchy

**Resolution Failures**
1. **IllegalStateException**: No resolver found for parameter
2. **IllegalArgumentException**: Parameter validation failures
3. **BadRequestException**: Client data format errors

**Error Context Preservation**

```java
throw new BadRequestException(
    "Failed to parse request body as JSON or convert to '" + 
    parameter.getType().getName() + "'. Check JSON format and target type. Cause: " + 
    e.getMessage(), ResponseCode.BAD_REQUEST, e);
```

**Recovery Strategies**

- **Fail-Fast**: Stop resolution on first error
- **Error Propagation**: Preserve original exception causes
- **HTTP Mapping**: Map internal errors to appropriate HTTP status codes

## Extensibility Analysis

### Adding New Resolvers

**Implementation Requirements**
1. Implement `ArgumentResolver` interface
2. Add `@Component` annotation for auto-registration
3. Define clear support criteria
4. Handle type conversion appropriately

**Example Custom Resolver**
```java
@Component
public class SessionArgumentResolver implements ArgumentResolver {
    @Override
    public boolean supports(Parameter parameter) {
        return parameter.isAnnotationPresent(SessionAttribute.class);
    }

    @Override
    public Object resolve(Parameter parameter, HttpRequest<?> request, 
                         Map<String, String> pathVariables) throws Exception {
        // Custom session handling logic
        SessionAttribute annotation = parameter.getAnnotation(SessionAttribute.class);
        return sessionManager.getAttribute(annotation.value());
    }
}
```

### Type Converter Extension

**Current Limitations**
- Fixed set of supported types
- No custom converter registration
- No complex object conversion

**Potential Enhancement**
```java
public interface TypeConverter {
    boolean supports(Class<?> sourceType, Class<?> targetType);
    Object convert(Object source, Class<?> targetType);
}

// Registry of converters
private final List<TypeConverter> converters;
```

## Integration with IoC Container

### Automatic Resolver Discovery

**Component Scanning**
```java
@Component
public class PathVariableArgumentResolver implements ArgumentResolver
```

**Dependency Injection**
```java
public CompositeArgumentResolver(List<ArgumentResolver> delegates) {
    this.delegates = delegates;
}
```

**IoC Benefits**
- Automatic resolver registration
- Ordered resolver injection via `@Order`
- Easy testing with mock resolvers

### Resolver Ordering

**Current Behavior**
- Order determined by IoC container bean creation order
- No explicit priority handling
- Could benefit from `@Order` annotation support

**Potential Enhancement**
```java
@Component
@Order(100)
public class PathVariableArgumentResolver implements ArgumentResolver
```

## Security Considerations

### Input Validation

**Current State**
- Basic type validation through conversion
- No input sanitization
- No size limits on input data

**Security Gaps**

1. **JSON Bomb Protection**: No limits on JSON parsing depth/size
2. **Path Variable Validation**: No regex validation on path variables
3. **Header Injection**: No validation of header content

**Improvement Considerations**

```java
// Size limits
public Object resolve(Parameter parameter, HttpRequest<?> request, ...) {
    String rawBody = (String) request.getBody();
    
    if (rawBody != null && rawBody.length() > MAX_BODY_SIZE) {
        throw new PayloadTooLargeException();
    }
    
    // Configure ObjectMapper with security settings
    objectMapper.configure(JsonParser.Feature.STRICT_DUPLICATE_DETECTION, true);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
}
```

### Type Safety Enforcement

**Primitive Type Protection**
```java
if (targetType.isPrimitive() && value == null) {
    throw new IllegalArgumentException("Null value cannot be assigned to primitive type");
}
```

**Class Loading Security**
- Uses reflection but doesn't allow arbitrary class instantiation
- Type conversion limited to known safe types
- Jackson deserialization could be configured with security settings

## Comparison with Spring MVC

### Similarities

- Annotation-driven parameter binding
- Extensible resolver chain architecture
- Type conversion system
- Support for common HTTP parameter types

### Differences

**Simplified Type System**
- Spring: Complex ConversionService with extensive type support
- Sprout: Simple TypeConverter with basic types

**Resolver Discovery**
- Spring: Complex HandlerMethodArgumentResolverComposite with ordering
- Sprout: Simple List-based iteration

**Error Handling**
- Spring: Sophisticated MethodArgumentResolutionException hierarchy
- Sprout: Basic exception types with HTTP status mapping

**Performance**
- Spring: Optimized with caching and pre-computed resolver mappings
- Sprout: Linear search through resolver list (optimization opportunity)

---

Sprout's argument resolution demonstrates a well-designed, extensible architecture that successfully abstracts HTTP request data binding. The system leverages proven design patterns (Composite, Strategy, Chain of Responsibility) while maintaining simplicity suitable for educational purposes.

**Strengths**
- Clear separation of concerns
- Extensible resolver architecture
- Type-safe parameter binding
- Good error handling with HTTP status mapping

**Areas for Enhancement**
- Performance optimization through resolver caching
- Enhanced type conversion system
- Security hardening for input validation
- Support for generic types and complex objects

Additional issue creation and contributions to these enhancement areas would contribute to the continued development of the Sprout framework.