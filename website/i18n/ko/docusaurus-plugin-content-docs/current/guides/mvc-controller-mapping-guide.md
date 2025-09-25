# 🌐 MVC 컨트롤러 매핑

Sprout Framework는 요청 라우팅, 패스 패턴 매칭, 핸들러 메서드 해결을 처리하는 유연하고 강력한 컨트롤러 매핑 시스템을 제공합니다. 이 시스템은 들어오는 HTTP 요청을 적절한 컨트롤러 메서드에 매핑하기 위해 함께 작동하는 여러 핵심 구성 요소를 중심으로 구축되었습니다.

## 아키텍처 개요

MVC 컨트롤러 매핑 시스템은 여러 핵심 구성 요소로 구성됩니다:

- **PathPattern**: 변수, 와일드카드, 정규식을 지원하는 고급 패턴 매칭
- **HandlerMethodScanner**: 컨트롤러 메서드를 발견하고 등록
- **RequestMappingRegistry**: 모든 요청 매핑의 중앙 레지스트리
- **HandlerMapping**: 들어오는 요청에 대해 가장 적합한 핸들러를 찾음
- **HandlerMethodInvoker**: 해결된 인수로 선택된 핸들러 메서드를 호출

## 패스 패턴 매칭

### PathPattern 클래스

`PathPattern` 클래스는 정교한 URL 패턴 매칭 기능을 제공합니다:

```java
public class PathPattern implements Comparable<PathPattern> {
    // 패턴: "/users/{id}/orders/{orderId}"
    // 매치: "/users/123/orders/456"
    
    public boolean matches(String path);
    public Map<String, String> extractPathVariables(String path);
}
```

### 지원되는 패턴 문법

#### 경로 변수
```java
@GetMapping("/users/{id}")
public User getUser(@PathVariable String id) { }

@GetMapping("/users/{id}/orders/{orderId}")
public Order getOrder(@PathVariable String id, @PathVariable String orderId) { }
```

#### 경로 변수에서 커스텀 정규식
```java
// 커스텀 정규식을 가진 패턴
@GetMapping("/users/{id:\\d+}")  // 숫자 ID만 매치
public User getUser(@PathVariable String id) { }
```

#### 와일드카드
```java
// 단일 와일드카드 - 하나의 경로 세그먼트 매치
@GetMapping("/files/*/download")  // 매치: /files/image.jpg/download

// 이중 와일드카드 - 여러 경로 세그먼트 매치
@GetMapping("/static/**")         // 매치: /static/css/main.css
```

#### 단일 문자 매칭
```java
@GetMapping("/files/?.txt")       // 매치: /files/a.txt, /files/1.txt
```

### 패턴 우선순위와 명시도

여러 패턴이 동일한 요청에 매치될 수 있을 때 패턴은 명시도에 따라 자동으로 정렬됩니다:

```java
@Override
public int compareTo(PathPattern other) {
    // 1. 이중 와일드카드(**)가 적을수록 더 명시적
    int c = Integer.compare(this.doubleStarCount, other.doubleStarCount);
    if (c != 0) return c;

    // 2. 단일 와일드카드(*)가 적을수록 더 명시적
    c = Integer.compare(this.singleStarCount, other.singleStarCount);
    if (c != 0) return c;

    // 3. 경로 변수가 적을수록 더 명시적
    c = Integer.compare(this.varNames.size(), other.varNames.size());
    if (c != 0) return c;

    // 4. 정적 콘텐츠가 길수록 더 명시적
    c = Integer.compare(other.staticLen, this.staticLen);
    if (c != 0) return c;

    // 5. 안정적인 정렬을 위한 사전식 순서
    return this.originalPattern.compareTo(other.originalPattern);
}
```

**우선순위 순서 예시:**
```java
"/users/admin"           // 가장 명시적 (정적 경로)
"/users/{id:\\d+}"       // 더 명시적 (제약된 변수)
"/users/{id}"            // 덜 명시적 (제약되지 않은 변수)
"/users/*"               // 덜 명시적 (단일 와일드카드)
"/**"                    // 가장 덜 명시적 (이중 와일드카드)
```

## 요청 매핑 등록

### 컨트롤러 스캔

`HandlerMethodScanner`는 컨트롤러 클래스와 핸들러 메서드를 자동으로 발견합니다:

```java
@Component
public class HandlerMethodScanner {
    public void scanControllers(BeanFactory context) {
        for (Object bean : context.getAllBeans()) {
            Class<?> beanClass = bean.getClass();
            if (beanClass.isAnnotationPresent(Controller.class)) {
                String classLevelBasePath = extractBasePath(beanClass);
                for (Method method : beanClass.getMethods()) {
                    // 요청 매핑 어노테이션에 대해 각 메서드 처리
                }
            }
        }
    }
}
```

### 경로 결합

클래스 레벨과 메서드 레벨 경로가 지능적으로 결합됩니다:

```java
@Controller
@RequestMapping("/api/users")
public class UserController {
    
    @GetMapping("/{id}")          // 결과: /api/users/{id}
    public User getUser(@PathVariable String id) { }
    
    @PostMapping("/")             // 결과: /api/users/
    public User createUser(@RequestBody User user) { }
}
```

### 어노테이션 처리

스캐너는 다양한 요청 매핑 어노테이션을 지원합니다:

```java
@RequestMapping(path = "/users", method = HttpMethod.GET)
@GetMapping("/users")           // 동일한 축약형
@PostMapping("/users")
@PutMapping("/users/{id}")
@DeleteMapping("/users/{id}")
@PatchMapping("/users/{id}")
```

## 요청 매핑 레지스트리

### 등록 과정

`RequestMappingRegistry`는 발견된 모든 매핑을 저장합니다:

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

### 핸들러 해결

요청이 들어오면 레지스트리는 가장 적합한 핸들러를 찾습니다:

```java
public RequestMappingInfo getHandlerMethod(String path, HttpMethod httpMethod) {
    List<RequestMappingInfo> matchingHandlers = new ArrayList<>();

    // 1. 요청 경로와 매치되는 모든 패턴 찾기
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

    // 2. 패턴 명시도에 따라 정렬 (가장 명시적인 것 우선)
    matchingHandlers.sort(Comparator.comparing(RequestMappingInfo::pattern));

    // 3. 가장 명시적인 매치 반환
    return matchingHandlers.get(0);
}
```

## 핸들러 메서드 호출

### 인수 해결

`HandlerMethodInvoker`는 메서드 인수를 해결하고 핸들러를 호출합니다:

```java
@Component
public class HandlerMethodInvoker {
    public Object invoke(RequestMappingInfo requestMappingInfo, 
                        HttpRequest<?> request) throws Exception {
        
        // 매치된 패턴에서 경로 변수 추출
        PathPattern pattern = requestMappingInfo.pattern();
        Map<String, String> pathVariables = pattern.extractPathVariables(request.getPath());

        // 모든 메서드 인수 해결
        Object[] args = resolvers.resolveArguments(
            requestMappingInfo.handlerMethod(), request, pathVariables);
        
        // 핸들러 메서드 호출
        return requestMappingInfo.handlerMethod()
            .invoke(requestMappingInfo.controller(), args);
    }
}
```

## 완전한 예제

모든 구성 요소가 함께 작동하는 방법을 보여주는 포괄적인 예제입니다:

### 컨트롤러 정의

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

### 생성된 매핑

스캐너는 각각의 패턴과 함께 이러한 매핑들을 등록합니다:

| HTTP 메서드 | 패턴 | 핸들러 메서드 | 우선순위 |
|-------------|------|---------------|----------|
| GET | `/api/v1/books` | `getAllBooks()` | 높음 (정적) |
| GET | `/api/v1/books/{id:\\d+}` | `getBook()` | 중간 (제약된 변수) |
| GET | `/api/v1/books/{category}/latest` | `getLatestBooksByCategory()` | 중간 (정적 + 변수) |
| POST | `/api/v1/books` | `createBook()` | 높음 (정적) |
| PUT | `/api/v1/books/{id}` | `updateBook()` | 낮음 (제약되지 않은 변수) |
| DELETE | `/api/v1/books/{id}` | `deleteBook()` | 낮음 (제약되지 않은 변수) |

### 요청 처리 흐름

1. **요청 도착**: `GET /api/v1/books/123`

2. **패턴 매칭**:
    - `/api/v1/books/{id:\\d+}` 매치 (숫자 ID에 대해 가장 명시적)
    - `/api/v1/books/{id}` 도 매치되지만 낮은 우선순위

3. **핸들러 선택**: `getBook()` 메서드 선택

4. **경로 변수 추출**: `{id: "123"}`

5. **인수 해결**: "123"을 `Long id = 123L`로 변환

6. **메서드 호출**: `bookController.getBook(123L)`

## 초기화와 생명주기

매핑 시스템은 애플리케이션 시작 중에 초기화됩니다:

```java
@Component
public class HandlerContextInitializer implements ContextInitializer {
    @Override
    public void initializeAfterRefresh(BeanFactory context) {
        scanner.scanControllers(context);
    }
}
```

이것은 서버가 요청을 받기 시작하기 전에 모든 컨트롤러가 발견되고 등록됨을 보장합니다.

## 모범 사례

### 1. 명시적인 패턴 사용
```java
// 좋음: 명시적인 패턴
@GetMapping("/users/{id:\\d+}")
public User getUser(@PathVariable Long id) { }

// 피해야 함: 너무 일반적
@GetMapping("/users/{id}")
public User getUser(@PathVariable String id) { }
```

### 2. 기본 경로로 구조화
```java
@Controller
@RequestMapping("/api/v1/users")
public class UserController {
    @GetMapping("/{id}")         // /api/v1/users/{id}
    @PostMapping("/")            // /api/v1/users/
    @PutMapping("/{id}")         // /api/v1/users/{id}
}
```

### 3. 모호한 매핑 처리
```java
// 충돌을 피하기 위해 제약 조건 사용
@GetMapping("/users/{id:\\d+}")      // 숫자 ID
@GetMapping("/users/{username}")     // 사용자명 (숫자가 아닌)
```

### 4. 일관된 HTTP 메서드 사용
```java
@GetMapping("/users")           // 목록/조회
@PostMapping("/users")          // 생성
@PutMapping("/users/{id}")      // 업데이트 (전체)
@PatchMapping("/users/{id}")    // 업데이트 (부분)
@DeleteMapping("/users/{id}")   // 삭제
```
