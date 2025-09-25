# 🎁 MVC Argument Resolution

## 개요

이 문서는 HTTP 요청 데이터를 컨트롤러 메서드 매개변수에 자동으로 바인딩하는 Sprout Framework의 인수 해결 시스템에 대한 기술적 분석을 제공합니다. 이 시스템은 유연하고 확장 가능한 해결자 체인을 통해 원시 HTTP 데이터(경로 변수, 헤더, 쿼리 매개변수, 요청 본문)를 강력한 타입의 메서드 인수로 변환합니다.

## 아키텍처 개요

### 인수 해결 흐름

```
HTTP 요청 데이터 → CompositeArgumentResolver → 특정 ArgumentResolver들
                                ↓
컨트롤러 메서드 매개변수 ← TypeConverter ← 해결된 인수들
```

### 구성 요소 상호작용

```
┌─────────────────────┐    ┌──────────────────────┐    ┌─────────────────┐
│   HTTP 요청         │───→│ CompositeArgument    │───→│ ArgumentResolver│
│   - 경로 변수       │    │ Resolver             │    │ 구현체들         │
│   - 헤더            │    │ (조정자)             │    │                 │
│   - 쿼리 매개변수   │    └──────────────────────┘    └─────────────────┘
│   - 본문            │                ↓                         ↓
└─────────────────────┘    ┌──────────────────────┐    ┌─────────────────┐
                           │   타입 변환기        │←───│ 해결된 값들     │
                           │   (타입 안전성)      │    │                 │
                           └──────────────────────┘    └─────────────────┘
```

## 핵심 구성 요소 분석

### 1. CompositeArgumentResolver: 해결 조정자

**위임 전략**

`CompositeArgumentResolver`는 여러 특화된 해결자들을 조정하기 위해 복합 패턴을 구현합니다.

```java
public Object[] resolveArguments(Method method, HttpRequest<?> request, 
                                Map<String, String> pathVariables) throws Exception {
    Parameter[] params = method.getParameters();
    Object[] args = new Object[params.length];
    
    for (int i = 0; i < params.length; i++) {
        Parameter p = params[i];
        
        // 첫 번째 지원 해결자 찾기
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

**설계 패턴 분석**

1. **책임 연쇄 패턴**: 각 해결자가 매개변수를 처리할 수 있는지 확인
2. **전략 패턴**: 다양한 매개변수 유형에 대한 서로 다른 해결 전략
3. **템플릿 메서드**: 특화된 구현과 함께하는 공통 해결 프레임워크

**성능 특성**

- **시간 복잡도**: O(n * m) (n = 매개변수 수, m = 평균 확인된 해결자 수)
- **조기 종료**: 첫 번째 일치하는 해결자에서 중단
- **캐싱 기회**: 반복 호출을 위한 해결자 매핑 캐시 가능

**오류 처리 전략**

```java
.orElseThrow(() -> new IllegalStateException("No ArgumentResolver for parameter " + p));
```

- 빠른 실패 접근: 알 수 없는 매개변수 유형은 즉시 실패
- 디버깅을 위한 명확한 오류 메시지
- 부분적 해결 시도 없음

### 2. ArgumentResolver 인터페이스: 해결자 계약

**계약 정의**

```java
public interface ArgumentResolver {
    boolean supports(Parameter parameter);
    Object resolve(Parameter parameter, HttpRequest<?> request, 
                  Map<String, String> pathVariables) throws Exception;
}
```

**2단계 해결 프로토콜**

1. **지원 확인**: 해결자가 매개변수를 처리할 수 있는지 결정
2. **해결**: 실제 값 추출 및 변환 수행

**인터페이스 설계 이점**

- **확장성**: 새로운 해결자 유형 추가 용이
- **테스트 가능성**: 각 해결자를 독립적으로 단위 테스트 가능
- **관심사 분리**: 명확한 책임 경계

### 3. TypeConverter: 중앙화된 타입 변환

**변환 알고리즘**

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

**타입 안전성 기능**

1. **기본 타입 널 검사**: 기본 타입에 널 할당 방지
2. **래퍼 타입 처리**: 기본 타입과 래퍼 타입 모두 지원
3. **명시적 타입 매핑**: 지원되는 타입에 대한 명확한 변환 규칙
4. **안전 장치**: 지원되지 않는 변환에 대한 예외 발생

**지원되는 변환**

| 소스 | 대상 타입 | 변환 메서드 |
|------|----------|-------------|
| String | String | 항등 |
| String | Long/long | Long.parseLong() |
| String | Integer/int | Integer.parseInt() |
| String | Boolean/boolean | Boolean.parseBoolean() |

## 해결자 구현 분석

### 1. PathVariableArgumentResolver

**지원 감지**

```java
public boolean supports(Parameter parameter) {
    return parameter.isAnnotationPresent(PathVariable.class);
}
```

**해결 로직**

```java
public Object resolve(Parameter parameter, HttpRequest<?> request, 
                     Map<String, String> pathVariables) throws Exception {
    PathVariable pathVariableAnnotation = parameter.getAnnotation(PathVariable.class);
    String variableName = pathVariableAnnotation.value();

    if (variableName.isEmpty()) {
        variableName = parameter.getName();  // 설정보다 관례
    }

    String value = pathVariables.get(variableName);
    if (value == null) {
        throw new IllegalArgumentException(
            "Path variable '" + variableName + "' not found in path.");
    }
    
    return TypeConverter.convert(value, parameter.getType());
}
```

**주요 기능**

- **설정보다 관례**: 어노테이션 값이 비어있으면 매개변수 이름 사용
- **엄격한 검증**: 경로 변수가 없으면 예외 발생
- **타입 변환**: 중앙화된 변환기에 위임

### 2. RequestParamArgumentResolver

**향상된 지원 로직**

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

**고급 기능**

- **선택적 매개변수**: 필수/선택적 구분 지원
- **널 처리**: 누락된 선택적 매개변수의 우아한 처리
- **검증 로직**: 필수 매개변수 제약 조건 강제

### 3. HeaderArgumentResolver

**이중 모드 해결**

시스템은 두 개의 헤더 해결자를 제공합니다.

**개별 헤더 해결**
```java
// HeaderArgumentResolver - 특정 헤더 처리
public boolean supports(Parameter parameter) {
    return parameter.isAnnotationPresent(Header.class) && 
           !parameter.getAnnotation(Header.class).value().isEmpty();
}
```

**모든 헤더 해결**
```java
// AllHeaderArgumentResolver - 헤더 맵 주입 처리
public boolean supports(Parameter parameter) {
    return parameter.isAnnotationPresent(Header.class) && 
           parameter.getAnnotation(Header.class).value().isEmpty();
}
```

**타입 기반 디스패치**

```java
if (parameter.getType().equals(Map.class)) {
    if (headerName.isBlank()) {
        return request.getHeaders();  // 모든 헤더 반환
    } else {
        throw new IllegalArgumentException(
            "Cannot bind specific header '" + headerName + 
            "' to a Map parameter. Use Map<String, String> without @Header for all headers.");
    }
}
```

### 4. RequestBodyArgumentResolver

**JSON 역직렬화 통합**

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

**고급 기능**

- **Jackson 통합**: JSON 역직렬화를 위한 ObjectMapper 사용
- **제네릭 타입 지원**: 모든 클래스로 역직렬화 가능
- **오류 컨텍스트**: 상세한 오류 정보 제공
- **HTTP 상태 매핑**: 파싱 오류를 적절한 HTTP 응답에 매핑

## 타입 시스템 통합

### Java 리플렉션 통합

**매개변수 내성**

```java
Parameter[] params = method.getParameters();
// 각 Parameter는 다음을 제공:
// - parameter.getType() - 타입 확인을 위한 Class<?>
// - parameter.getName() - 설정보다 관례를 위한 String
// - parameter.getAnnotations() - 메타데이터를 위한 Annotation[]
// - parameter.isAnnotationPresent(Class) - 빠른 확인을 위한 boolean
```

**타입 소거 처리**

시스템은 현재 기본 타입을 처리하지만 제네릭 타입에는 제한이 있습니다.

```java
// 작동함
public void method(@RequestBody User user)

// 제한적 - 제네릭 타입 정보 손실
public void method(@RequestBody List<User> users)
```

**잠재적 개선**

```java
// 제네릭 지원을 위해 ParameterizedType 사용 가능
if (parameter.getParameterizedType() instanceof ParameterizedType) {
    ParameterizedType pType = (ParameterizedType) parameter.getParameterizedType();
    Type[] actualTypeArguments = pType.getActualTypeArguments();
    // List<User>, Map<String, Object> 등 처리
}
```

## 성능 분석

### 해결 복잡도

**요청당 해결**
- 시간: O(p * r) (p = 매개변수, r = 평균 확인할 해결자 수)
- 공간: O(p) (인수 배열 할당)
- 최적화: 해결자 캐싱 구현 가능

**타입 변환 오버헤드**
- 기본 타입 변환: O(1)
- 문자열 연산: O(1)
- JSON 역직렬화: O(json_크기)

### 메모리 사용 패턴

**해결자 체인**
```java
private final List<ArgumentResolver> delegates;
```
- 모든 요청에서 공유되는 정적 해결자 목록
- 요청당 해결자 할당 없음

**인수 배열**
```java
Object[] args = new Object[params.length];
```
- 메서드 호출당 임시 배열
- 메서드 시그니처로 크기 결정

### 최적화 기회

**해결자 매핑 캐시**
```java
// 잠재적 개선
private final Map<Parameter, ArgumentResolver> resolverCache = new ConcurrentHashMap<>();

public Object[] resolveArguments(...) {
    // 매개변수당 해결자 매핑 캐시
    ArgumentResolver resolver = resolverCache.computeIfAbsent(p, 
        param -> delegates.stream().filter(ar -> ar.supports(param)).findFirst().orElse(null));
}
```

## 오류 처리 전략

### 예외 계층

**해결 실패**
1. **IllegalStateException**: 매개변수에 대한 해결자를 찾을 수 없음
2. **IllegalArgumentException**: 매개변수 검증 실패
3. **BadRequestException**: 클라이언트 데이터 형식 오류

**오류 컨텍스트 보존**

```java
throw new BadRequestException(
    "Failed to parse request body as JSON or convert to '" + 
    parameter.getType().getName() + "'. Check JSON format and target type. Cause: " + 
    e.getMessage(), ResponseCode.BAD_REQUEST, e);
```

**복구 전략**

- **빠른 실패**: 첫 번째 오류에서 해결 중단
- **오류 전파**: 원본 예외 원인 보존
- **HTTP 매핑**: 내부 오류를 적절한 HTTP 상태 코드로 매핑

## 확장성 분석

### 새로운 해결자 추가

**구현 요구사항**
1. `ArgumentResolver` 인터페이스 구현
2. 자동 등록을 위한 `@Component` 어노테이션 추가
3. 명확한 지원 기준 정의
4. 적절한 타입 변환 처리

**사용자 정의 해결자 예제**
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
        // 사용자 정의 세션 처리 로직
        SessionAttribute annotation = parameter.getAnnotation(SessionAttribute.class);
        return sessionManager.getAttribute(annotation.value());
    }
}
```

### 타입 변환기 확장

**현재 제한사항**
- 고정된 지원 타입 집합
- 사용자 정의 변환기 등록 없음
- 복잡한 객체 변환 없음

**잠재적 개선**
```java
public interface TypeConverter {
    boolean supports(Class<?> sourceType, Class<?> targetType);
    Object convert(Object source, Class<?> targetType);
}

// 변환기 레지스트리
private final List<TypeConverter> converters;
```

## IoC 컨테이너와의 통합

### 자동 해결자 발견

**컴포넌트 스캔**
```java
@Component
public class PathVariableArgumentResolver implements ArgumentResolver
```

**의존성 주입**
```java
public CompositeArgumentResolver(List<ArgumentResolver> delegates) {
    this.delegates = delegates;
}
```

**IoC 이점**
- 자동 해결자 등록
- `@Order`를 통한 순서화된 해결자 주입
- 모의 해결자로 쉬운 테스트

### 해결자 순서

**현재 동작**
- IoC 컨테이너 빈 생성 순서로 결정되는 순서
- 명시적 우선순위 처리 없음
- `@Order` 어노테이션 지원으로 이익을 얻을 수 있음

**잠재적 개선**
```java
@Component
@Order(100)
public class PathVariableArgumentResolver implements ArgumentResolver
```

## 보안 고려사항

### 입력 검증

**현재 상태**
- 변환을 통한 기본 타입 검증
- 입력 정화 없음
- 입력 데이터 크기 제한 없음

**보안 격차**

1. **JSON 폭탄 보호**: JSON 파싱 깊이/크기 제한 없음
2. **경로 변수 검증**: 경로 변수에 대한 정규식 검증 없음
3. **헤더 인젝션**: 헤더 내용 검증 없음

**개선 고려사항**

```java
// 크기 제한
public Object resolve(Parameter parameter, HttpRequest<?> request, ...) {
    String rawBody = (String) request.getBody();
    
    if (rawBody != null && rawBody.length() > MAX_BODY_SIZE) {
        throw new PayloadTooLargeException();
    }
    
    // 보안 설정으로 ObjectMapper 구성
    objectMapper.configure(JsonParser.Feature.STRICT_DUPLICATE_DETECTION, true);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
}
```

### 타입 안전성 강제

**기본 타입 보호**
```java
if (targetType.isPrimitive() && value == null) {
    throw new IllegalArgumentException("Null value cannot be assigned to primitive type");
}
```

**클래스 로딩 보안**
- 리플렉션을 사용하지만 임의 클래스 인스턴스화는 허용하지 않음
- 알려진 안전한 타입으로 제한된 타입 변환
- 보안 설정으로 구성할 수 있는 Jackson 역직렬화 구성

## Spring MVC와의 비교

### 유사점

- 어노테이션 기반 매개변수 바인딩
- 확장 가능한 해결자 체인 아키텍처
- 타입 변환 시스템
- 일반적인 HTTP 매개변수 유형 지원

### 차이점

**단순화된 타입 시스템**
- Spring: 광범위한 타입 지원을 가진 복잡한 ConversionService
- Sprout: 기본 타입을 가진 간단한 TypeConverter

**해결자 발견**
- Spring: 순서화를 가진 복잡한 HandlerMethodArgumentResolverComposite
- Sprout: 간단한 목록 기반 반복

**오류 처리**
- Spring: 정교한 MethodArgumentResolutionException 계층
- Sprout: HTTP 상태 매핑을 가진 기본 예외 유형

**성능**
- Spring: 캐싱과 미리 계산된 해결자 매핑으로 최적화
- Sprout: 해결자 목록을 통한 선형 검색 (최적화 여지가 있습니다)

---

Sprout의 argument resolution은 HTTP 요청 데이터 바인딩을 성공적으로 추상화하는 잘 설계되고 확장 가능한 아키텍처를 보여줍니다. 이 시스템은 교육적 목적에 적합한 단순함을 유지하면서 검증된 설계 패턴(복합, 전략, 책임 연쇄)을 활용합니다.

**강점**
- 명확한 관심사 분리
- 확장 가능한 해결자 아키텍처
- 타입 안전한 매개변수 바인딩
- HTTP 상태 매핑을 가진 좋은 오류 처리

**개선 영역**
- 해결자 캐싱을 통한 성능 최적화
- 향상된 타입 변환 시스템
- 입력 검증을 위한 보안 강화
- 제네릭 타입과 복잡한 객체 지원

개선 영역에 대한 추가적 이슈 발행과 기여는 Sprout 프레임워크의 지속적인 발전에 기여할 것입니다.