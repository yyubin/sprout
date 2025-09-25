# 🎢 Sprout MVC 컨트롤러 매핑

## 개요

이 문서는 Sprout Framework의 MVC 컨트롤러 매핑 구현에 대한 기술적 분석을 제공하며, HTTP 요청을 컨트롤러 메서드로 라우팅하는 데 사용되는 내부 아키텍처, 알고리즘, 설계 패턴을 검토합니다.

## 핵심 아키텍처

### 구성 요소 상호작용 흐름

```
HTTP 요청 → HandlerMapping → RequestMappingRegistry → PathPattern 매칭
                                      ↓
컨트롤러 발견 ← HandlerMethodScanner ← BeanFactory
                                      ↓
핸들러 호출 ← HandlerMethodInvoker ← 인수 해결
```

### 핵심 구성 요소 분석

#### 1. PathPattern: 고급 패턴 매칭 엔진

**구현 세부사항**

`PathPattern` 클래스는 커스텀 파싱 로직과 함께 Java의 정규식 엔진을 사용하는 정교한 URL 패턴 매칭 시스템을 구현합니다.

```java
public class PathPattern implements Comparable<PathPattern> {
    private final String originalPattern;
    private final Pattern regex;                    // 매칭을 위한 컴파일된 정규식
    private final List<String> varNames;           // 순서대로 변수 이름들
    private final List<Integer> varGroups;         // 해당 정규식 그룹들
    private final int staticLen;                   // 정적 콘텐츠의 길이
    private final int singleStarCount;             // * 와일드카드 개수
    private final int doubleStarCount;             // ** 와일드카드 개수
}
```

**패턴 컴파일 알고리즘**

생성자는 다양한 토큰 유형을 처리하는 상태 머신을 구현합니다.

1. **변수 토큰** (`{name}` 또는 `{name:regex}`)
    - `VAR_TOKEN` 패턴 사용: `\\{([^/:}]+)(?::([^}]+))?}`
    - 변수 이름과 선택적 커스텀 정규식 추출
    - 제약되지 않은 변수에 대해 `[^/]+` 기본값 사용

2. **와일드카드 처리**
    - `*` → `([^/]+)` (단일 경로 세그먼트)
    - `**` → `(.+?)` (여러 세그먼트, non-greedy)
    - `?` → `[^/]` (단일 문자)

3. **정적 콘텐츠**: `Pattern.quote()`를 사용하여 이스케이프 처리

**명시도 알고리즘**

`compareTo` 메서드는 다중 기준 정렬 알고리즘을 구현합니다.

```java
public int compareTo(PathPattern other) {
    // 우선순위 순서 (오름차순 = 더 명시적):
    // 1. ** 와일드카드가 적을수록
    // 2. * 와일드카드가 적을수록  
    // 3. 경로 변수가 적을수록
    // 4. 정적 콘텐츠가 길수록
    // 5. 사전식 패턴 문자열
}
```

이는 가장 명시적인 패턴이 먼저 매치되도록 하여, 광범위한 패턴이 구체적인 패턴을 가리는 것을 방지합니다.

#### 2. HandlerMethodScanner: 리플렉션 기반 발견

**스캐닝 전략**

스캐너는 다단계 접근 방식을 사용합니다.

1. **빈 열거**: 컨테이너의 모든 빈을 반복
2. **컨트롤러 감지**: `@Controller` 어노테이션 확인
3. **메서드 내성**: 매핑 어노테이션에 대한 모든 공개 메서드 검사
4. **어노테이션 처리**: 메타 어노테이션과 상속 처리

**어노테이션 해결 알고리즘**

```java
public RequestMappingInfoExtractor findRequestMappingInfoExtractor(Method method) {
    for (Annotation ann : method.getDeclaredAnnotations()) {
        // 직접 @RequestMapping 또는 메타 어노테이션된 매핑 확인
        RequestMapping rm = ann instanceof RequestMapping 
            ? (RequestMapping) ann 
            : ann.annotationType().getAnnotation(RequestMapping.class);
        
        if (rm == null) continue;
        
        // 폴백 계층과 함께 경로 추출: value() → path() → fallback
        String[] paths = extractPaths(ann, rm);
        HttpMethod[] methods = rm.method();
        
        return new RequestMappingInfoExtractor(path, methods);
    }
    return null;
}
```

**경로 결합 로직**

`combinePaths` 메서드는 URL 구성의 엣지 케이스를 처리합니다.

```java
public String combinePaths(String basePath, String methodPath) {
    // 다음과 같은 경우들을 처리:
    // ("", "/users") → "/users"
    // ("/api", "users") → "/api/users"  
    // ("/api/", "/users") → "/api/users"
    // ("/api", "/") → "/api"
}
```

#### 3. RequestMappingRegistry: 동시성 매핑 저장소

**데이터 구조 설계**

```java
private final Map<PathPattern, Map<HttpMethod, RequestMappingInfo>> mappings 
    = new ConcurrentHashMap<>();
```

이 중첩된 맵 구조는 다음을 제공합니다.
- **스레드 안전성**: 동시 접근을 위한 `ConcurrentHashMap`
- **효율적인 조회**: 패턴 매칭 후 HTTP 메서드로 O(1) 접근
- **메모리 효율성**: HTTP 메서드에 대한 `EnumMap`으로 메모리 오버헤드 감소

**핸들러 해결 알고리즘**

`getHandlerMethod` 구현은 3단계 접근 방식을 사용합니다.

1. **패턴 매칭 단계**
   ```java
   for (PathPattern registeredPattern : mappings.keySet()) {
       if (registeredPattern.matches(path)) {
           // 매칭되는 핸들러들 수집
       }
   }
   ```

2. **정렬 단계**
   ```java
   matchingHandlers.sort(Comparator.comparing(RequestMappingInfo::pattern));
   ```

3. **선택 단계**: 첫 번째(가장 명시적인) 매치 반환

**성능 특성**
- 시간 복잡도: O(n * m) (n = 등록된 패턴 수, m = 평균 정규식 복잡도)
- 공간 복잡도: O(p * h) (p = 패턴 수, h = 패턴당 HTTP 메서드 수)
- 최적화: 매치가 없을 때 조기 종료

#### 4. HandlerMethodInvoker: 메서드 실행 엔진

**호출 파이프라인**

```java
public Object invoke(RequestMappingInfo requestMappingInfo, HttpRequest<?> request) {
    // 1. 경로 변수 추출
    Map<String, String> pathVariables = pattern.extractPathVariables(request.getPath());
    
    // 2. 복합 리졸버를 통한 인수 해결
    Object[] args = resolvers.resolveArguments(handlerMethod, request, pathVariables);
    
    // 3. 리플렉티브 메서드 호출
    return handlerMethod.invoke(controller, args);
}
```

이 설계는 관심사를 분리하고 확장 가능한 인수 해결 전략을 허용합니다.

## 고급 기술 기능

### 1. 메타 어노테이션 지원

프레임워크는 Spring 스타일 메타 어노테이션을 지원합니다.

```java
@RequestMapping(method = HttpMethod.GET)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GetMapping {
    String[] value() default {};
    String[] path() default {};
}
```

스캐너는 어노테이션 분석을 통해 이를 감지합니다.

```java
RequestMapping rm = ann.annotationType().getAnnotation(RequestMapping.class);
```

### 2. 정규식 컴파일 최적화

패턴은 등록 시 한 번 컴파일되고 모든 매칭 작업에 재사용됩니다.

```java
this.regex = Pattern.compile(re.toString());
```

이는 요청 처리 중 반복적인 패턴 컴파일 오버헤드를 방지합니다.

### 3. 변수 그룹 매핑

구현은 변수 이름과 정규식 캡처 그룹 사이의 병렬 매핑을 유지합니다.

```java
private final List<String> varNames;      // ["id", "category"]
private final List<Integer> varGroups;    // [1, 2]
```

이는 문자열 파싱 없이 효율적인 변수 추출을 가능하게 합니다.

## 성능 분석

### 초기화 성능

**컨트롤러 스캐닝 복잡도**
- 시간: O(C * M * A) (C = 컨트롤러 수, M = 컨트롤러당 메서드 수, A = 메서드당 어노테이션 수)
- 공간: O(P) (P = 전체 등록된 패턴 수)
- 최적화: 시작 시 한 번 수행, 결과 캐시됨

### 런타임 성능

**요청 라우팅 복잡도**
- 최적의 경우: 고유한 정적 패턴으로 O(1)
- 평균적인 경우: 잘 분산된 패턴으로 O(log P)
- 최악의 경우: O(P * R) (P = 패턴 수, R = 정규식 복잡도)

**메모리 사용량**
- 패턴 저장: 패턴당 ~200-500바이트 (정규식 + 메타데이터)
- 레지스트리 오버헤드: 매핑당 ~100바이트
- 총합: 1000개 미만의 엔드포인트를 가진 애플리케이션에 대해 일반적으로 `<1MB` 가능

### 최적화 전략

1. **패턴 순서**: 가장 명시적인 패턴을 먼저 확인
2. **지연 컴파일**: 패턴당 한 번만 정규식 컴파일
3. **효율적인 데이터 구조**: HTTP 메서드에 대한 EnumMap, 순서화된 컬렉션에 대한 ArrayList
4. **단락 평가**: 첫 번째 매치에서 조기 반환

## 설계 패턴과 원칙

### 1. 전략 패턴
- `CompositeArgumentResolver`가 특정 리졸버에 위임
- 핵심 로직 수정 없이 확장 가능한 인수 해결 허용

### 2. 템플릿 메서드 패턴
- `HandlerMethodScanner`가 스캐닝 알고리즘 정의
- 하위 클래스가 특정 단계를 재정의 가능

### 3. 레지스트리 패턴
- `RequestMappingRegistry`가 매핑 저장을 중앙화
- 등록과 조회를 위한 통합된 인터페이스 제공

### 4. Comparable/Comparator 패턴
- `PathPattern`이 명시도별 자연 순서 구현
- 외부 로직 없이 자동 정렬 가능

## 오류 처리 및 엣지 케이스

### 1. 모호한 매핑
```java
if (cnt != 1) {
    System.out.printf("[WARN] %s.%s() - skipped: ambiguous @RequestMapping", 
                      method.getDeclaringClass().getSimpleName(), method.getName());
    return null;
}
```

### 2. 잘못된 패턴 구문
```java
if (!varMatcher.region(i, pattern.length()).lookingAt()) {
    throw new IllegalArgumentException("Invalid variable syntax at index " + i);
}
```

### 3. 핸들러 없음
```java
if (matchingHandlers.isEmpty()) {
    return null;
}
```

시스템은 명확한 오류 메시지와 폴백 동작으로 이러한 시나리오를 처리합니다.

## Spring MVC와의 비교

### 유사점
- 어노테이션 기반 구성
- 변수와 와일드카드를 가진 패턴 매칭
- 핸들러 메서드 내성
- 인수 해결 파이프라인

### 차이점
- **단순화된 아키텍처**: 추상화 레이어가 적음
- **정규식 기반 매칭**: Spring의 AntPathMatcher 대신 직접 정규식 컴파일
- **구성 가능성 감소**: 일반적인 사용 사례에 집중
- **성능 집중**: 유연성보다 속도에 최적화

## 확장성 포인트

### 1. 커스텀 인수 리졸버
`ArgumentResolver` 인터페이스를 구현하고 `CompositeArgumentResolver`에 등록

### 2. 커스텀 패턴 매칭
추가 구문을 지원하도록 `PathPattern` 확장

### 3. 커스텀 핸들러 발견
대안적인 `HandlerMethodScanner` 전략 구현

### 4. 커스텀 매핑 저장
대안적인 저장 메커니즘으로 `RequestMappingRegistry` 교체
