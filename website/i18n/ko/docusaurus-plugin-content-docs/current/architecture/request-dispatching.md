# 🚀 Request Dispatching

## 개요

이 문서는 Sprout Framework의 요청 디스패칭 시스템에 대한 심층적인 기술 분석을 제공합니다. HTTP 요청이 파싱된 후부터 컨트롤러 메서드 실행, 응답 생성까지의 전체 처리 파이프라인을 검토하며, Filter와 Interceptor의 작동 메커니즘, Spring Framework와의 차이점, 그리고 Sprout만의 고유한 설계 결정들을 상세히 분석합니다.

## 디스패칭 파이프라인 아키텍처

### 전체 요청 처리 흐름

```
HttpRequest → RequestDispatcher → 컨트롤러 메서드 → HttpResponse
                ↓
┌─────────────────────────────────────────────────────────────────────┐
│ DispatchHook → FilterChain → InterceptorChain → HandlerInvoker     │
│                                      ↓                               │
│ ResponseResolver ← ResponseAdvice ← 반환값                          │
└─────────────────────────────────────────────────────────────────────┘
```

### 처리 단계별 실행 순서

**요청 단계**:
1. `DispatchHook.beforeDispatch()` - 전처리 훅
2. `FilterChain.doFilter()` - 필터 체인 실행
3. `InterceptorChain.applyPreHandle()` - 인터셉터 전처리
4. `HandlerMethodInvoker.invoke()` - 컨트롤러 메서드 실행
5. `InterceptorChain.applyPostHandle()` - 인터셉터 후처리

**응답 단계**:
1. `ResponseAdvice.beforeBodyWrite()` - 응답 어드바이스
2. `ResponseResolver.resolve()` - 응답 해결
3. `InterceptorChain.applyAfterCompletion()` - 인터셉터 완료 처리
4. `DispatchHook.afterDispatch()` - 후처리 훅

## 핵심 구성 요소 분석

### 1. RequestDispatcher: 중앙 조정자

**의존성 주입 구조**

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
```

**설계 원칙**

1. **의존성 역전**: 구체적인 구현체가 아닌 인터페이스에 의존
2. **컴포지션 패턴**: 여러 전략 객체들을 조합하여 복잡한 로직 구성
3. **단일 책임**: 디스패칭 조정에만 집중, 실제 처리는 전용 컴포넌트에 위임
4. **확장성**: List 기반으로 여러 구현체 지원

### 2. 메인 디스패치 로직 분석

**dispatch() 메서드: 최상위 진입점**

```java
public void dispatch(HttpRequest<?> req, HttpResponse res) throws IOException {
    try {
        // 1. 전처리 훅 실행
        for (DispatchHook hook : dispatchHooks) {
            hook.beforeDispatch(req, res);
        }

        // 2. 필터 체인과 실제 디스패치 로직 연결
        new FilterChain(filters, this::doDispatch).doFilter(req, res);
    } finally {
        // 3. 후처리 훅 실행 (반드시 실행)
        for (DispatchHook hook : dispatchHooks) {
            hook.afterDispatch(req, res);
        }
    }
}
```

**핵심 설계 특징**

1. **try-finally 패턴**: 예외 발생 여부와 관계없이 후처리 훅 실행 보장
2. **함수형 인터페이스 활용**: `this::doDispatch`로 메서드 레퍼런스 전달
3. **계층화된 실행**: 훅 → 필터 → 실제 디스패치 순서

**doDispatch() 메서드: 핵심 비즈니스 로직**

```java
private void doDispatch(HttpRequest<?> req, HttpResponse res) {
    HandlerMethod hm = null;
    Exception caughtException = null;
    InterceptorChain interceptorChain = new InterceptorChain(interceptors);

    try {
        // 1. 핸들러 매핑
        hm = mapping.findHandler(req.getPath(), req.getMethod());
        if (hm == null) {
            // 404 응답 직접 생성
            res.setResponseEntity(
                new ResponseEntity<>("Not Found", null, ResponseCode.NOT_FOUND)
            );
            return;
        }

        // 2. 인터셉터 전처리
        if (!interceptorChain.applyPreHandle(req, res, hm)) {
            return; // 인터셉터가 요청을 중단한 경우
        }

        // 3. 컨트롤러 메서드 실행
        Object returnValue = invoker.invoke(hm.requestMappingInfo(), req);

        // 4. 인터셉터 후처리
        interceptorChain.applyPostHandle(req, res, hm, returnValue);

        // 5. 응답 처리
        setResponseResolvers(returnValue, req, res);

    } catch (Exception e) {
        caughtException = e;
        // 6. 예외 처리 위임
        handleException(req, res, hm, e);
    } finally {
        // 7. 인터셉터 완료 처리
        if (hm != null) {
            interceptorChain.applyAfterCompletion(req, res, hm, caughtException);
        }
    }
}
```

### 3. 예외 처리 전략

**계층화된 예외 해결 메커니즘**

```java
// ExceptionResolver 체인을 통한 예외 처리
Object handledReturnValue = null;
for (ExceptionResolver resolver : exceptionResolvers) {
    handledReturnValue = resolver.resolveException(req, res, hm, caughtException);
    if (handledReturnValue != null) {
        // 처리 성공 시 응답 설정
        if (handledReturnValue instanceof ResponseEntity) {
            res.setResponseEntity((ResponseEntity<?>) handledReturnValue);
        } else {
            setResponseResolvers(handledReturnValue, req, res);
        }
        return;
    }
}
```

**예외 처리 설계 특징**

1. **체인 오브 리스펀서빌리티**: 여러 리졸버가 순차적으로 예외 처리 시도
2. **조기 종료**: 첫 번째 처리 성공 시 나머지 리졸버 실행 안함
3. **유연한 반환값**: `ResponseEntity` 직접 반환 또는 일반 객체를 통한 추가 처리
4. **타입 안전성**: `instanceof` 검사를 통한 런타임 타입 체크

### 4. 응답 처리 파이프라인

**ResponseResolver와 ResponseAdvice 협력**

```java
private void setResponseResolvers(Object returnValue, HttpRequest<?> req, HttpResponse res) {
    if (res.isCommitted()) return; // 중복 처리 방지

    // 1. ResponseAdvice 체인 적용
    Object processed = applyResponseAdvices(returnValue, req);

    // 2. 적절한 ResponseResolver 탐색 및 적용
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

**ResponseAdvice 체인 처리**

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

## Filter 시스템 분석

### FilterChain 구현

**체인 오브 리스펀서빌리티 패턴**

```java
public class FilterChain {
    private final List<Filter> filters;
    private final Dispatcher dispatcher;
    private int currentFilterIndex = 0;

    public void doFilter(HttpRequest<?> request, HttpResponse response) throws IOException {
        if (currentFilterIndex < filters.size()) {
            // 다음 필터 실행
            filters.get(currentFilterIndex++).doFilter(request, response, this);
            return;
        }
        // 모든 필터 완료 후 실제 디스패처 호출
        dispatcher.dispatch(request, response);
    }
}
```

**FilterChain 특징**

1. **상태 기반 진행**: `currentFilterIndex`로 현재 실행 위치 추적
2. **재귀적 호출**: 각 필터가 다음 체인을 직접 호출
3. **함수형 인터페이스**: `Dispatcher`를 통한 최종 처리 위임
4. **선형 실행**: 필터들이 순차적으로 실행됨

### Filter 인터페이스

```java
public interface Filter extends InfrastructureBean {
    void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) throws IOException;
}
```

**Filter vs Servlet Filter 비교**

| 특성 | Sprout Filter | Servlet Filter |
|------|---------------|----------------|
| 매개변수 | `HttpRequest`, `HttpResponse`, `FilterChain` | `ServletRequest`, `ServletResponse`, `FilterChain` |
| 체크 예외 | `IOException` | `IOException`, `ServletException` |
| 생명주기 | Spring 빈 생명주기 | 서블릿 컨테이너 관리 |
| 설정 | `@Component` + DI | `web.xml` 또는 `@WebFilter` |

### 실제 Filter 구현 예시: CorsFilter

**설정 기반 CORS 처리**

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
            chain.doFilter(request, response); // Origin 없으면 CORS 처리 건너뛰기
            return;
        }

        // CORS 헤더 설정
        applyCorsHeaders(response, origin);

        // OPTIONS 프리플라이트 요청 처리
        if (request.getMethod().equals(HttpMethod.OPTIONS)) {
            handlePreflightRequest(request, response);
            return; // 체인 진행 중단
        }

        chain.doFilter(request, response); // 다음 체인 계속
    }
}
```

## Interceptor 시스템 분석

### InterceptorChain 구현

**순차적 실행과 역순 정리**

```java
public class InterceptorChain {
    private final List<Interceptor> interceptors;

    public boolean applyPreHandle(HttpRequest request, HttpResponse response, Object handler) {
        for (Interceptor interceptor : interceptors) {
            if (!interceptor.preHandle(request, response, handler)) {
                return false; // 하나라도 false 반환 시 중단
            }
        }
        return true;
    }

    public void applyPostHandle(HttpRequest request, HttpResponse response, Object handler, Object result) {
        // 역순으로 실행 (LIFO)
        for (int i = interceptors.size() - 1; i >= 0; i--) {
            interceptors.get(i).postHandle(request, response, handler, result);
        }
    }

    public void applyAfterCompletion(HttpRequest request, HttpResponse response, Object handler, Exception ex) {
        // 역순으로 실행 (LIFO)
        for (int i = interceptors.size() - 1; i >= 0; i--) {
            interceptors.get(i).afterCompletion(request, response, handler, ex);
        }
    }
}
```

**Interceptor 실행 패턴**

1. **preHandle**: 순방향 실행, 하나라도 `false` 반환 시 전체 중단
2. **postHandle**: 역방향 실행 (LIFO), 컨트롤러 실행 성공 후에만 실행
3. **afterCompletion**: 역방향 실행 (LIFO), 예외 발생 여부 무관하게 항상 실행

### Interceptor vs Filter 비교

| 특성 | Interceptor | Filter |
|------|-------------|---------|
| **실행 시점** | 핸들러 매핑 후 | 핸들러 매핑 전 |
| **접근 정보** | 핸들러 정보 접근 가능 | 핸들러 정보 접근 불가 |
| **실행 단계** | 3단계 (pre/post/after) | 1단계 (doFilter) |
| **처리 범위** | 컨트롤러 메서드 중심 | HTTP 요청 전체 |
| **실행 순서** | 후처리는 역순 (LIFO) | 항상 순차적 |
| **중단 메커니즘** | boolean 반환값 | 체인 호출 안함 |

## HandlerMethodInvoker 분석

### 메서드 실행 전략

```java
@Component
public class HandlerMethodInvoker {
    private final CompositeArgumentResolver resolvers;

    public Object invoke(RequestMappingInfo requestMappingInfo, HttpRequest<?> request) throws Exception {
        PathPattern pattern = requestMappingInfo.pattern();

        // 1. URL 패턴에서 경로 변수 추출
        Map<String, String> pathVariables = pattern.extractPathVariables(request.getPath());

        // 2. 컨트롤러 메서드 파라미터 해결
        Object[] args = resolvers.resolveArguments(requestMappingInfo.handlerMethod(), request, pathVariables);

        // 3. 리플렉션을 통한 메서드 실행
        return requestMappingInfo.handlerMethod().invoke(requestMappingInfo.controller(), args);
    }
}
```

**핵심 처리 과정**

1. **경로 변수 추출**: `PathPattern`을 통해 URL에서 `{id}` 같은 변수 추출
2. **인수 해결**: `CompositeArgumentResolver`로 메서드 파라미터 값 결정
3. **메서드 호출**: Java 리플렉션 API로 실제 컨트롤러 메서드 실행

## ControllerAdvice 기반 예외 처리 시스템

Sprout은 Spring의 `@ControllerAdvice`와 `@ExceptionHandler` 어노테이션을 모방한 예외 처리 시스템을 제공합니다.

### ControllerAdviceRegistry: 예외 핸들러 등록소

**예외 핸들러 스캔과 등록**

```java
@Component
public class ControllerAdviceRegistry {
    private final List<ExceptionHandlerObject> allExceptionHandlers = new ArrayList<>();
    private final Map<Class<? extends Throwable>, Optional<ExceptionHandlerObject>> cachedHandlers = new ConcurrentHashMap<>();

    public void scanControllerAdvices(BeanFactory context) {
        Collection<Object> allBeans = context.getAllBeans();
        for (Object bean : allBeans) {
            if (bean.getClass().isAnnotationPresent(ControllerAdvice.class)) {
                // @ControllerAdvice 클래스 발견
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

**핵심 설계 특징**

1. **리플렉션 기반 스캔**: 런타임에 `@ControllerAdvice` 빈들을 탐색
2. **메서드 접근성**: `setAccessible(true)`로 private 핸들러 메서드도 호출 가능
3. **캐싱 메커니즘**: `ConcurrentHashMap`으로 예외 타입별 핸들러 캐시

### 예외 핸들러 매칭 알고리즘

**최적 핸들러 선택 전략**

```java
private Optional<ExceptionHandlerObject> lookupBestMatchHandler(Class<? extends Throwable> exceptionClass) {
    ExceptionHandlerObject bestMatch = null;
    int bestMatchDistance = Integer.MAX_VALUE;

    for (ExceptionHandlerObject handler : allExceptionHandlers) {
        Method handlerMethod = handler.getMethod();
        for (Class<? extends Throwable> handledExceptionType : handlerMethod.getAnnotation(ExceptionHandler.class).value()) {
            if (handledExceptionType.isAssignableFrom(exceptionClass)) {
                // 예외 계층 구조에서 거리 계산
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
    if (fromClass.equals(toClass)) return 0; // 정확한 타입 매치

    int distance = 0;
    Class<?> current = toClass;
    while (current != null && !current.equals(fromClass)) {
        current = current.getSuperclass();
        distance++;
    }
    return (current != null) ? distance : Integer.MAX_VALUE;
}
```

**매칭 알고리즘 특징**

1. **거리 기반 매칭**: 예외 타입 계층에서 가장 가까운(구체적인) 핸들러 선택
2. **다중 예외 지원**: `@ExceptionHandler({Exception1.class, Exception2.class})`
3. **상속 관계 고려**: 부모 예외 핸들러가 하위 예외도 처리 가능
4. **정확한 매치 우선**: 동일한 타입일 때 거리 0으로 최우선 선택

### ControllerAdviceExceptionResolver 구현

**예외 해결 및 메서드 호출**

```java
@Component
@Order(0)  // 최우선 실행
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

            // 다양한 메서드 시그니처 지원
            Object handlerReturnValue = invokeExceptionHandler(handlerMethodRef, handlerInstance, exception, request);

            // ResponseResolver를 통한 응답 처리
            return processHandlerReturnValue(handlerReturnValue, request, response);
        }
        return null; // 처리하지 못함
    }
}
```

**지원하는 메서드 시그니처 패턴**

```java
// 1. 매개변수 없음
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<String> handleIllegalArgument() { }

// 2. 예외만 받기
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) { }

// 3. 예외 + 요청
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e, HttpRequest request) { }

// 4. 요청 + 예외 (순서 바뀜)
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<String> handleIllegalArgument(HttpRequest request, IllegalArgumentException e) { }
```

**메서드 호출 전략**

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
        // 매개변수 순서에 관계없이 타입으로 판단
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

### ExceptionHandlerObject: 핸들러 메타데이터

**단순한 메타데이터 홀더**

```java
public class ExceptionHandlerObject {
    private final Method method;
    private final Object bean;

    public ExceptionHandlerObject(Method method, Object bean) {
        this.method = method;
        this.bean = bean;
        method.setAccessible(true); // private 메서드도 호출 가능
    }
}
```

### 어노테이션 정의

**ControllerAdvice 어노테이션**

```java
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ControllerAdvice {
    // 클래스 레벨에만 적용
}
```

**ExceptionHandler 어노테이션**

```java
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ExceptionHandler {
    Class<? extends Throwable>[] value(); // 처리할 예외 타입 배열
}
```

### 전체 예외 처리 흐름

**RequestDispatcher에서의 통합**

```java
// doDispatch() 메서드 내 예외 처리 부분
catch (Exception e) {
    caughtException = e;

    Object handledReturnValue = null;
    for (ExceptionResolver resolver : exceptionResolvers) {
        // ControllerAdviceExceptionResolver가 @Order(0)으로 최우선 실행
        handledReturnValue = resolver.resolveException(req, res, hm, caughtException);
        if (handledReturnValue != null) {
            // @ExceptionHandler에서 처리됨
            setResponseResolvers(handledReturnValue, req, res);
            return;
        }
    }
    // 처리되지 않은 예외는 기본 처리
}
```

**실행 순서**

1. **컨트롤러에서 예외 발생**
2. **ControllerAdviceExceptionResolver 실행** (최우선)
3. **ControllerAdviceRegistry에서 핸들러 검색**
4. **가장 구체적인 핸들러 선택**
5. **핸들러 메서드 호출**
6. **ResponseResolver로 응답 변환**
7. **처리 실패 시 다음 ExceptionResolver로 이동**

### Spring과의 차이점 비교

**ControllerAdvice 기능 비교**

| 특성 | Spring @ControllerAdvice | Sprout @ControllerAdvice |
|------|-------------------------|--------------------------|
| **적용 범위** | 전역 또는 패키지/클래스 지정 | 전역만 지원 |
| **어노테이션 위치** | 클래스 레벨 | 클래스 레벨 |
| **스캔 방식** | 컴포넌트 스캔 자동 | BeanFactory에서 수동 스캔 |
| **메서드 시그니처** | 매우 유연 (Model, WebRequest 등) | 제한적 (Exception, HttpRequest) |
| **응답 처리** | ViewResolver, HttpMessageConverter | ResponseResolver |
| **캐싱** | 기본 제공 | 수동 구현 (ConcurrentHashMap) |

**ExceptionHandler 기능 비교**

| 특성 | Spring @ExceptionHandler | Sprout @ExceptionHandler |
|------|-------------------------|--------------------------|
| **매개변수 지원** | 20+ 종류 (Model, HttpSession 등) | 4가지 패턴만 지원 |
| **반환값 지원** | ResponseEntity, Model, View 등 | ResponseResolver 의존 |
| **예외 타입 매칭** | 런타임 해결 | 컴파일타임 + 런타임 |
| **순서 제어** | @Order 지원 | 거리 기반 알고리즘 |
| **비동기 지원** | DeferredResult, Callable | 미지원 |

### 성능 및 메모리 특성

**캐싱 전략**

```java
// 예외 타입별 핸들러 캐싱으로 성능 최적화
private final Map<Class<? extends Throwable>, Optional<ExceptionHandlerObject>> cachedHandlers = new ConcurrentHashMap<>();

public Optional<ExceptionHandlerObject> getExceptionHandler(Class<? extends Throwable> exceptionClass) {
    return cachedHandlers.computeIfAbsent(exceptionClass, this::lookupBestMatchHandler);
}
```

**시간 복잡도**

- **첫 번째 조회**: O(n × m) (n = 핸들러 수, m = 처리 가능한 예외 타입 수)
- **캐시 적중**: O(1)
- **예외 거리 계산**: O(d) (d = 상속 계층 깊이)

**메모리 사용**

- **핸들러 저장**: 각 `@ExceptionHandler` 메서드마다 `ExceptionHandlerObject` 인스턴스
- **캐시 저장**: 조회된 예외 타입마다 `Optional<ExceptionHandlerObject>` 저장
- **메서드 접근성**: `setAccessible(true)` 호출로 보안 검사 비용 절약

## Spring Framework와의 비교 분석

### 아키텍처 차이점

**Spring DispatcherServlet vs Sprout RequestDispatcher**

| 측면 | Spring DispatcherServlet | Sprout RequestDispatcher |
|------|-------------------------|--------------------------|
| **기반 기술** | Servlet API 기반 | 순수 Java 기반 |
| **생명주기** | 서블릿 컨테이너 관리 | Spring 빈 생명주기 |
| **초기화** | `init()`, `destroy()` | 생성자 주입 |
| **예외 처리** | `HandlerExceptionResolver` | `ExceptionResolver` |
| **뷰 해결** | `ViewResolver` | `ResponseResolver` |

### 필터 시스템 차이점

**실행 위치와 범위**

```java
// Spring: 서블릿 컨테이너 레벨
public class SpringFilter implements Filter {
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        // 서블릿 컨테이너에서 실행
        // DispatcherServlet 이전에 실행
    }
}

// Sprout: 애플리케이션 레벨
public class SproutFilter implements Filter {
    public void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) {
        // RequestDispatcher에서 직접 실행
        // 애플리케이션 컨텍스트 내에서 실행
    }
}
```

**라이프사이클 관리**

- **Spring**: 서블릿 컨테이너가 필터 생명주기 관리, `@WebFilter` 또는 `web.xml` 설정
- **Sprout**: Spring IoC 컨테이너가 관리, `@Component`로 자동 등록

### Interceptor 구현 차이점

**HandlerInterceptor vs Interceptor**

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

**주요 차이점**

1. **매개변수 타입**: Servlet API vs 자체 HTTP 추상화
2. **ModelAndView**: Spring은 뷰 모델 전달, Sprout은 일반 반환값 전달
3. **기본 구현**: Spring은 `default` 메서드, Sprout은 모든 메서드 구현 필요

## 성능 분석

### 시간 복잡도

**요청 처리 과정별 복잡도**

- **DispatchHook 실행**: O(h) (h = 훅 개수)
- **Filter 체인**: O(f) (f = 필터 개수)
- **핸들러 매핑**: O(log n) (n = 등록된 핸들러 수, 트리 탐색)
- **Interceptor 체인**: O(i) (i = 인터셉터 개수)
- **인수 해결**: O(p) (p = 메서드 파라미터 수)
- **메서드 실행**: O(1) (리플렉션 호출)
- **응답 처리**: O(r + a) (r = 리졸버 수, a = 어드바이스 수)

**전체 시간 복잡도**: O(h + f + log n + i + p + r + a)

### 메모리 사용 패턴

**객체 생성과 GC 압박**

```java
// 매 요청마다 생성되는 객체들
InterceptorChain interceptorChain = new InterceptorChain(interceptors);  // 체인 객체
Map<String, String> pathVariables = pattern.extractPathVariables(request.getPath());  // 경로 변수 맵
Object[] args = resolvers.resolveArguments(handlerMethod, request, pathVariables);  // 인수 배열
```

**최적화 전략**

1. **체인 객체 풀링**: InterceptorChain 재사용
2. **인수 배열 캐싱**: 동일한 메서드에 대한 배열 재사용
3. **경로 변수 최적화**: 빈 맵일 때 싱글톤 사용

### 병렬 처리 고려사항

**스레드 안전성**

- **RequestDispatcher**: 상태 없는 컴포넌트, 스레드 안전
- **FilterChain**: `currentFilterIndex` 상태 보유, 요청별 인스턴스 필요
- **InterceptorChain**: 상태 없음, 스레드 안전

**동시성 최적화**

```java
// 현재: 매 요청마다 새 체인 생성
new FilterChain(filters, this::doDispatch).doFilter(req, res);

// 최적화: ThreadLocal 기반 체인 재사용 가능
private final ThreadLocal<FilterChain> chainCache = ThreadLocal.withInitial(() -> new FilterChain(filters, this::doDispatch));
```

## 확장성과 유지보수성

### 새로운 처리 단계 추가

**확장 포인트**

1. **DispatchHook**: 요청 전/후 처리 로직 추가
2. **Filter**: HTTP 레벨 전처리 (인증, 로깅, CORS 등)
3. **Interceptor**: 컨트롤러 레벨 전처리 (권한 검사, 로깅 등)
4. **ExceptionResolver**: 예외 처리 전략 추가
5. **ResponseResolver**: 새로운 응답 형식 지원

### 설정과 자동 구성

**빈 등록과 순서 제어**

```java
@Configuration
public class WebConfig {
    @Bean
    @Order(1)  // 실행 순서 제어
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

### 테스트 가능성

**단위 테스트 전략**

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

## 보안 고려사항

### 현재 보안 메커니즘

**1. 핸들러 매핑 보안**
- 핸들러가 없는 경우 404 응답 반환
- 직접적인 예외 노출 방지

**2. 예외 처리 보안**
- 스택 트레이스 콘솔 출력 (운영 환경 부적절)
- ExceptionResolver를 통한 안전한 예외 변환

### 보안 개선 사항

**1. 정보 노출 방지**

```java
// 현재: 디버깅 정보 노출
System.err.println("Exception caught in doDispatch: " + e.getMessage());
e.printStackTrace();

// 개선: 로깅 레벨 기반 제어
if (logger.isDebugEnabled()) {
    logger.debug("Exception in doDispatch", e);
} else {
    logger.error("Request processing failed: {}", request.getPath());
}
```

**2. 입력 검증 강화**

```java
// 권장: 요청 크기 제한
public void dispatch(HttpRequest<?> req, HttpResponse res) {
    validateRequest(req);  // 크기, 헤더 개수 등 검증
    // ... 기존 로직
}
```

**3. CSRF/XSS 방지**

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
---

취약점 및 개선점에 대한 새로운 의견 및 제안, PR 환영합니다!