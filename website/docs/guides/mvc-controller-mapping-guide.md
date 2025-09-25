# 🌐 MVC Controller Mapping

Sprout Framework provides a flexible and powerful controller mapping system that handles request routing, path pattern matching, and handler method resolution. The system is built around several key components that work together to map incoming HTTP requests to appropriate controller methods.

## Architecture Overview

The MVC controller mapping system consists of several key components:

- **PathPattern**: Advanced pattern matching with variables, wildcards, and regex support
- **HandlerMethodScanner**: Discovers and registers controller methods
- **RequestMappingRegistry**: Central registry for all request mappings
- **HandlerMapping**: Finds the best matching handler for incoming requests
- **HandlerMethodInvoker**: Invokes the selected handler method with resolved arguments

## Path Pattern Matching

### PathPattern Class

The `PathPattern` class provides sophisticated URL pattern matching capabilities:

```java
public class PathPattern implements Comparable<PathPattern> {
    // Pattern: "/users/{id}/orders/{orderId}"
    // Matches: "/users/123/orders/456"
    
    public boolean matches(String path);
    public Map<String, String> extractPathVariables(String path);
}
```

### Supported Pattern Syntax

#### Path Variables
```java
@GetMapping("/users/{id}")
public User getUser(@PathVariable String id) { }

@GetMapping("/users/{id}/orders/{orderId}")
public Order getOrder(@PathVariable String id, @PathVariable String orderId) { }
```

#### Custom Regex in Path Variables
```java
// Pattern with custom regex
@GetMapping("/users/{id:\\d+}")  // Only matches numeric IDs
public User getUser(@PathVariable String id) { }
```

#### Wildcards
```java
// Single wildcard - matches one path segment
@GetMapping("/files/*/download")  // Matches: /files/image.jpg/download

// Double wildcard - matches multiple path segments
@GetMapping("/static/**")         // Matches: /static/css/main.css
```

#### Single Character Matching
```java
@GetMapping("/files/?.txt")       // Matches: /files/a.txt, /files/1.txt
```

### Pattern Priority and Specificity

Patterns are automatically sorted by specificity when multiple patterns could match the same request:

```java
@Override
public int compareTo(PathPattern other) {
    // 1. Fewer double wildcards (**) are more specific
    int c = Integer.compare(this.doubleStarCount, other.doubleStarCount);
    if (c != 0) return c;

    // 2. Fewer single wildcards (*) are more specific
    c = Integer.compare(this.singleStarCount, other.singleStarCount);
    if (c != 0) return c;

    // 3. Fewer path variables are more specific
    c = Integer.compare(this.varNames.size(), other.varNames.size());
    if (c != 0) return c;

    // 4. Longer static content is more specific
    c = Integer.compare(other.staticLen, this.staticLen);
    if (c != 0) return c;

    // 5. Lexicographic order for stable sorting
    return this.originalPattern.compareTo(other.originalPattern);
}
```

**Example Priority Order:**
```java
"/users/admin"           // Most specific (static path)
"/users/{id:\\d+}"       // More specific (constrained variable)
"/users/{id}"            // Less specific (unconstrained variable)
"/users/*"               // Less specific (single wildcard)
"/**"                    // Least specific (double wildcard)
```

## Request Mapping Registration

### Controller Scanning

The `HandlerMethodScanner` automatically discovers controller classes and their handler methods:

```java
@Component
public class HandlerMethodScanner {
    public void scanControllers(BeanFactory context) {
        for (Object bean : context.getAllBeans()) {
            Class<?> beanClass = bean.getClass();
            if (beanClass.isAnnotationPresent(Controller.class)) {
                String classLevelBasePath = extractBasePath(beanClass);
                for (Method method : beanClass.getMethods()) {
                    // Process each method for request mapping annotations
                }
            }
        }
    }
}
```

### Path Combination

Class-level and method-level paths are intelligently combined:

```java
@Controller
@RequestMapping("/api/users")
public class UserController {
    
    @GetMapping("/{id}")          // Results in: /api/users/{id}
    public User getUser(@PathVariable String id) { }
    
    @PostMapping("/")             // Results in: /api/users/
    public User createUser(@RequestBody User user) { }
}
```

### Annotation Processing

The scanner supports various request mapping annotations:

```java
@RequestMapping(path = "/users", method = HttpMethod.GET)
@GetMapping("/users")           // Equivalent shorthand
@PostMapping("/users")
@PutMapping("/users/{id}")
@DeleteMapping("/users/{id}")
@PatchMapping("/users/{id}")
```

## Request Mapping Registry

### Registration Process

The `RequestMappingRegistry` stores all discovered mappings:

```java
@Component
public class RequestMappingRegistry {
    private final Map<PathPattern, Map<HttpMethod, RequestMappingInfo>> mappings;
    
    public void register(PathPattern pathPattern, HttpMethod httpMethod, 
                        Object controller, Method handlerMethod) {
        mappings.computeIfAbsent(pathPattern, k -> new EnumMap<>(HttpMethod.class))
                .put(httpMethod, new RequestMappingInfo(pathPattern, httpMethod, 
                     controller, handlerMethod));
    }
}
```

### Handler Resolution

When a request comes in, the registry finds the best matching handler:

```java
public RequestMappingInfo getHandlerMethod(String path, HttpMethod httpMethod) {
    List<RequestMappingInfo> matchingHandlers = new ArrayList<>();

    // 1. Find all patterns that match the request path
    for (PathPattern registeredPattern : mappings.keySet()) {
        if (registeredPattern.matches(path)) {
            Map<HttpMethod, RequestMappingInfo> methodMappings = mappings.get(registeredPattern);
            if (methodMappings != null && methodMappings.containsKey(httpMethod)) {
                matchingHandlers.add(methodMappings.get(httpMethod));
            }
        }
    }

    if (matchingHandlers.isEmpty()) {
        return null;
    }

    // 2. Sort by pattern specificity (most specific first)
    matchingHandlers.sort(Comparator.comparing(RequestMappingInfo::pattern));

    // 3. Return the most specific match
    return matchingHandlers.get(0);
}
```

## Handler Method Invocation

### Argument Resolution

The `HandlerMethodInvoker` resolves method arguments and invokes the handler:

```java
@Component
public class HandlerMethodInvoker {
    public Object invoke(RequestMappingInfo requestMappingInfo, 
                        HttpRequest<?> request) throws Exception {
        
        // Extract path variables from the matched pattern
        PathPattern pattern = requestMappingInfo.pattern();
        Map<String, String> pathVariables = pattern.extractPathVariables(request.getPath());

        // Resolve all method arguments
        Object[] args = resolvers.resolveArguments(
            requestMappingInfo.handlerMethod(), request, pathVariables);
        
        // Invoke the handler method
        return requestMappingInfo.handlerMethod()
            .invoke(requestMappingInfo.controller(), args);
    }
}
```

## Complete Example

Here's a comprehensive example showing how all components work together:

### Controller Definition

```java
@Controller
@RequestMapping("/api/v1")
public class BookController {

    @GetMapping("/books")
    public List<Book> getAllBooks() {
        return bookService.findAll();
    }

    @GetMapping("/books/{id:\\d+}")
    public Book getBook(@PathVariable Long id) {
        return bookService.findById(id);
    }

    @GetMapping("/books/{category}/latest")
    public List<Book> getLatestBooksByCategory(@PathVariable String category) {
        return bookService.findLatestByCategory(category);
    }

    @PostMapping("/books")
    public Book createBook(@RequestBody Book book) {
        return bookService.save(book);
    }

    @PutMapping("/books/{id}")
    public Book updateBook(@PathVariable Long id, @RequestBody Book book) {
        return bookService.update(id, book);
    }

    @DeleteMapping("/books/{id}")
    public void deleteBook(@PathVariable Long id) {
        bookService.delete(id);
    }
}
```

### Generated Mappings

The scanner will register these mappings with their respective patterns:

| HTTP Method | Pattern | Handler Method | Priority |
|-------------|---------|----------------|----------|
| GET | `/api/v1/books` | `getAllBooks()` | High (static) |
| GET | `/api/v1/books/{id:\\d+}` | `getBook()` | Medium (constrained var) |
| GET | `/api/v1/books/{category}/latest` | `getLatestBooksByCategory()` | Medium (static + var) |
| POST | `/api/v1/books` | `createBook()` | High (static) |
| PUT | `/api/v1/books/{id}` | `updateBook()` | Low (unconstrained var) |
| DELETE | `/api/v1/books/{id}` | `deleteBook()` | Low (unconstrained var) |

### Request Processing Flow

1. **Request arrives**: `GET /api/v1/books/123`

2. **Pattern matching**:
    - `/api/v1/books/{id:\\d+}` matches (most specific for numeric ID)
    - `/api/v1/books/{id}` also matches but has lower priority

3. **Handler selection**: `getBook()` method is selected

4. **Path variable extraction**: `{id: "123"}`

5. **Argument resolution**: Convert "123" to `Long id = 123L`

6. **Method invocation**: `bookController.getBook(123L)`

## Initialization and Lifecycle

The mapping system is initialized during application startup:

```java
@Component
public class HandlerContextInitializer implements ContextInitializer {
    @Override
    public void initializeAfterRefresh(BeanFactory context) {
        scanner.scanControllers(context);
    }
}
```

This ensures all controllers are discovered and registered before the server starts accepting requests.

## Best Practices

### 1. Use Specific Patterns
```java
// Good: Specific pattern
@GetMapping("/users/{id:\\d+}")
public User getUser(@PathVariable Long id) { }

// Avoid: Too generic
@GetMapping("/users/{id}")
public User getUser(@PathVariable String id) { }
```

### 2. Organize with Base Paths
```java
@Controller
@RequestMapping("/api/v1/users")
public class UserController {
    @GetMapping("/{id}")         // /api/v1/users/{id}
    @PostMapping("/")            // /api/v1/users/
    @PutMapping("/{id}")         // /api/v1/users/{id}
}
```

### 3. Handle Ambiguous Mappings
```java
// Use constraints to avoid conflicts
@GetMapping("/users/{id:\\d+}")      // Numeric IDs
@GetMapping("/users/{username}")     // Username (non-numeric)
```

### 4. Consistent HTTP Method Usage
```java
@GetMapping("/users")           // List/retrieve
@PostMapping("/users")          // Create
@PutMapping("/users/{id}")      // Update (full)
@PatchMapping("/users/{id}")    // Update (partial)
@DeleteMapping("/users/{id}")   // Delete
```
