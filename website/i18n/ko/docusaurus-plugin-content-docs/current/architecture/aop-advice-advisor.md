# 🎯 AOP Advice & Advisor 

## 개요

이 문서는 Sprout Framework의 AOP(Aspect-Oriented Programming) 핵심 구성 요소인 Advice와 Advisor 시스템에 대한 심층적인 기술 분석을 제공합니다. 어드바이스의 생성과 저장, 포인트컷 매칭 전략, 그리고 어드바이저 레지스트리의 내부 구조를 상세히 분석하여 Sprout AOP의 설계 철학과 구현 메커니즘을 이해할 수 있도록 합니다.

## AOP 아키텍처 개요

### 핵심 구성 요소 관계도

```
@Aspect 클래스
    ↓
AdviceFactory → AdviceBuilder → Advice (인터셉터)
    ↓                ↓              ↓
DefaultAdvisor ← Pointcut ← PointcutFactory
    ↓
AdvisorRegistry (저장 및 매칭)
    ↓
프록시 생성 시 사용
```

### 주요 컴포넌트 역할

- **Advice**: 실제 부가 기능을 실행하는 인터셉터
- **Advisor**: Advice + Pointcut + 실행 순서를 담는 컨테이너
- **Pointcut**: 어드바이스가 적용될 조인 포인트를 결정하는 조건
- **AdviceFactory**: 어노테이션을 분석하여 적절한 Advisor 생성
- **AdvisorRegistry**: 생성된 Advisor들을 저장하고 메서드별 적용 가능한 Advisor 검색

## Advice 시스템 분석

### 1. Advice 인터페이스: 통합된 인터셉션 모델

**단순하고 강력한 인터페이스**

```java
public interface Advice {
    Object invoke(MethodInvocation invocation) throws Throwable;
}
```

**설계 특징**

1. **단일 메서드**: 모든 어드바이스 타입이 동일한 시그니처 사용
2. **MethodInvocation 기반**: Spring의 Interceptor 패턴과 유사
3. **예외 투명성**: Throwable을 통한 모든 예외 전파
4. **체이닝 지원**: `invocation.proceed()`를 통한 다음 어드바이스 호출

### 2. AdviceType: 어드바이스 타입 분류 시스템

**열거형 기반 타입 관리**

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

**핵심 설계 결정**

1. **어노테이션과 타입 매핑**: 각 AdviceType이 해당 어노테이션 클래스 보유
2. **스트림 기반 검색**: Java 8+ 스트림 API로 간결한 타입 탐지
3. **Optional 반환**: null 안전성 보장
4. **확장 가능성**: 새로운 어드바이스 타입 추가 용이

### 3. AdviceFactory: 어드바이스 생성의 중앙 집권화

**팩토리 패턴과 전략 패턴 결합**

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

**아키텍처 특징**

1. **불변 빌더 맵**: `Map.of()`를 통한 컴파일 타임 빌더 매핑
2. **의존성 주입**: PointcutFactory를 생성자 주입으로 받음
3. **타입 안전성**: 제네릭과 Optional을 통한 타입 안전성 보장
4. **단일 책임**: 어드바이스 생성만 담당, 실제 구현은 빌더에 위임

### 4. AdviceBuilder 구현체들

#### BeforeAdviceBuilder: 사전 처리 어드바이스

**파라미터 검증과 빌더 생성**

```java
public class BeforeAdviceBuilder implements AdviceBuilder {
    @Override
    public Advisor build(Class<?> aspectCls, Method method, Supplier<Object> aspectSup, PointcutFactory pf) {
        Before before = method.getAnnotation(Before.class);

        // 1. 파라미터 검증
        if (method.getParameterCount() > 1 ||
            (method.getParameterCount() == 1 &&
             !JoinPoint.class.isAssignableFrom(method.getParameterTypes()[0]))) {
            throw new IllegalStateException("@Before method must have 0 or 1 JoinPoint param");
        }

        // 2. 포인트컷 생성
        Pointcut pc = pf.createPointcut(before.annotation(), before.pointcut());

        // 3. static 메서드 처리
        Supplier<Object> safe = Modifier.isStatic(method.getModifiers()) ? () -> null : aspectSup;

        // 4. 어드바이스와 어드바이저 생성
        Advice advice = new SimpleBeforeInterceptor(safe, method);
        return new DefaultAdvisor(pc, advice, 0);
    }
}
```

#### AroundAdviceBuilder: 완전한 제어 어드바이스

**ProceedingJoinPoint 필수 검증**

```java
public class AroundAdviceBuilder implements AdviceBuilder {
    @Override
    public Advisor build(Class<?> aspectCls, Method method, Supplier<Object> sup, PointcutFactory pf) {
        Around around = method.getAnnotation(Around.class);

        // ProceedingJoinPoint 필수 검증
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

**Around 어드바이스의 특징**

1. **엄격한 시그니처**: 정확히 하나의 ProceedingJoinPoint 파라미터만 허용
2. **완전한 제어**: 원본 메서드 호출 여부와 시점을 어드바이스에서 결정
3. **반환값 조작**: 원본 메서드 반환값을 가로채고 변경 가능

### 5. Advice 인터셉터 구현체들

#### SimpleBeforeInterceptor: 사전 실행 인터셉터

**사전 실행 후 원본 메서드 호출**

```java
public class SimpleBeforeInterceptor implements Advice {
    private final Supplier<Object> aspectProvider;
    private final Method adviceMethod;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // 1. aspect 인스턴스 획득 (static이면 null)
        Object aspect = java.lang.reflect.Modifier.isStatic(adviceMethod.getModifiers())
                ? null : aspectProvider.get();

        try {
            // 2. 어드바이스 메서드 실행
            if (adviceMethod.getParameterCount() == 0) {
                adviceMethod.invoke(aspect);
            } else {
                JoinPoint jp = new JoinPointAdapter(invocation);
                adviceMethod.invoke(aspect, jp);
            }
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }

        // 3. 원본 메서드 실행
        return invocation.proceed();
    }
}
```

#### SimpleAfterInterceptor: 사후 실행 인터셉터

**예외 상황을 고려한 After 처리**

```java
public class SimpleAfterInterceptor implements Advice {
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object result;
        Throwable thrown = null;

        try {
            // 1. 원본 메서드 먼저 실행
            result = invocation.proceed();
        } catch (Throwable t) {
            thrown = t;
            result = null;
        }

        // 2. After 어드바이스 실행 (예외 발생 여부와 관계없이)
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

        // 3. 원본 예외가 있으면 다시 던지기
        if (thrown != null) throw thrown;
        return result;
    }
}
```

**After 어드바이스의 핵심 특징**

1. **예외 무관 실행**: try-catch로 예외 포착 후 어드바이스 실행
2. **예외 보존**: 원본 메서드의 예외를 어드바이스 실행 후 재전파
3. **finally 시맨틱스**: Java의 finally 블록과 유사한 동작

#### SimpleAroundInterceptor: 완전 제어 인터셉터

**ProceedingJoinPoint를 통한 완전한 제어**

```java
public class SimpleAroundInterceptor implements Advice {
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // 1. ProceedingJoinPoint 어댑터 생성
        ProceedingJoinPoint pjp = new PjpAdapter(invocation);

        // 2. aspect 인스턴스 획득
        Object aspect = java.lang.reflect.Modifier.isStatic(adviceMethod.getModifiers())
                ? null : aspectProvider.get();

        try {
            // 3. Around 어드바이스 메서드 실행 (원본 메서드 호출 제어권 넘김)
            adviceMethod.setAccessible(true);
            return adviceMethod.invoke(aspect, pjp);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
}
```

**Around의 특별함**

1. **호출 제어**: 어드바이스가 `pjp.proceed()` 호출 여부 결정
2. **반환값 제어**: 원본 메서드 반환값을 가로채고 변경 가능
3. **예외 처리**: try-catch로 원본 메서드 예외 처리 가능

## Advisor 시스템 분석

### 1. Advisor 인터페이스: Advice와 Pointcut의 결합

**단순하고 명확한 컨트랙트**

```java
public interface Advisor {
    Pointcut getPointcut();
    Advice getAdvice();
    default int getOrder() {
        return Integer.MAX_VALUE; // 기본값, 가장 낮은 우선순위
    }
}
```

**설계 철학**

1. **합성 패턴**: Advice와 Pointcut을 조합하여 완전한 어드바이스 단위 구성
2. **순서 지원**: getOrder()로 여러 어드바이스의 실행 순서 제어
3. **기본값 제공**: 순서를 지정하지 않으면 가장 낮은 우선순위

### 2. DefaultAdvisor: 표준 어드바이저 구현

**불변 객체로 설계된 어드바이저**

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

**불변성의 이점**

1. **스레드 안전성**: 생성 후 상태 변경 불가로 멀티스레드 환경에서 안전
2. **예측 가능성**: 한번 생성된 어드바이저의 동작이 변경되지 않음
3. **캐싱 친화적**: 상태가 변경되지 않아 캐싱 전략에 유리

### 3. AdvisorRegistry: 어드바이저 저장소와 매처

**동시성을 고려한 레지스트리 설계**

```java
@Component
public class AdvisorRegistry implements InfrastructureBean {
    private final List<Advisor> advisors = new ArrayList<>();
    private final Map<Method, List<Advisor>> cachedAdvisors = new ConcurrentHashMap<>();

    public void registerAdvisor(Advisor advisor) {
        synchronized (this) {
            advisors.add(advisor);
            cachedAdvisors.clear();  // 캐시 무효화
            advisors.sort(Comparator.comparingInt(Advisor::getOrder));  // 순서 정렬
        }
    }

    public List<Advisor> getApplicableAdvisors(Class<?> targetClass, Method method) {
        List<Advisor> cached = cachedAdvisors.get(method);

        if (cached != null) {
            return cached;  // 캐시 적중
        }

        // 적용 가능한 어드바이저 검색
        List<Advisor> applicableAdvisors = new ArrayList<>();
        for (Advisor advisor : advisors) {
            if (advisor.getPointcut().matches(targetClass, method)) {
                applicableAdvisors.add(advisor);
            }
        }

        cachedAdvisors.put(method, applicableAdvisors);  // 결과 캐싱
        return applicableAdvisors;
    }
}
```

**핵심 최적화 전략**

1. **메서드별 캐싱**: `ConcurrentHashMap`으로 메서드별 적용 가능한 어드바이저 캐싱
2. **순서 정렬**: 등록 시점에 order 기준으로 정렬하여 실행 시 정렬 비용 절약
3. **캐시 무효화**: 새 어드바이저 등록 시 캐시 전체 클리어
4. **동기화 최소화**: 등록은 synchronized, 조회는 ConcurrentHashMap 활용

### 4. Pointcut 시스템

#### Pointcut 인터페이스

**단순하고 강력한 매칭 인터페이스**

```java
public interface Pointcut {
    boolean matches(Class<?> targetClass, Method method);
}
```

#### AnnotationPointcut: 어노테이션 기반 매칭

**계층적 어노테이션 검색**

```java
public class AnnotationPointcut implements Pointcut {
    private final Class<? extends Annotation> annotationType;

    @Override
    public boolean matches(Class<?> targetClass, Method method) {
        // 1. 메서드에 직접 어노테이션 존재
        if (has(method)) return true;

        // 2. 클래스 레벨에 어노테이션 존재 (선언 클래스 및 실제 타겟 클래스)
        if (has(method.getDeclaringClass()) || has(targetClass)) return true;

        return false;
    }

    private boolean has(AnnotatedElement el) {
        return el.isAnnotationPresent(annotationType);
    }
}
```

**매칭 우선순위**

1. **메서드 레벨**: 메서드에 직접 붙은 어노테이션 최우선
2. **클래스 레벨**: 메서드 선언 클래스 및 실제 타겟 클래스의 어노테이션

#### AspectJPointcutAdapter: AspectJ 표현식 지원

**AspectJ 라이브러리 통합**

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
        // 1. 클래스 레벨 사전 필터링
        if (!expression.couldMatchJoinPointsInType(targetClass)) {
            return false;
        }

        // 2. 메서드 실행 조인포인트 매칭
        var sm = expression.matchesMethodExecution(method);
        return sm.alwaysMatches() || sm.maybeMatches();
    }
}
```

**AspectJ 통합의 이점**

1. **강력한 표현력**: AspectJ의 풍부한 포인트컷 표현식 지원
2. **성능 최적화**: 클래스 레벨 사전 필터링으로 불필요한 메서드 체크 방지
3. **표준 호환성**: AspectJ 표준 문법 완전 지원

#### CompositePointcut: OR 조합 포인트컷

**여러 포인트컷의 논리합**

```java
public class CompositePointcut implements Pointcut {
    private final List<Pointcut> pointcuts;

    @Override
    public boolean matches(Class<?> targetClass, Method method) {
        for (Pointcut pointcut : pointcuts) {
            if (pointcut.matches(targetClass, method)) {
                return true;  // 하나라도 매치되면 true
            }
        }
        return false;
    }
}
```

### 5. PointcutFactory: 포인트컷 생성 전략

**복합 조건 포인트컷 생성**

```java
@Component
public class DefaultPointcutFactory implements PointcutFactory, InfrastructureBean {

    @Override
    public Pointcut createPointcut(Class<? extends Annotation>[] annotationTypes, String aspectjExpr) {
        List<Pointcut> pcs = new ArrayList<>();

        // 1. 어노테이션 조건들 추가
        if (annotationTypes != null && annotationTypes.length > 0) {
            for (Class<? extends Annotation> anno : annotationTypes) {
                pcs.add(new AnnotationPointcut(anno));
            }
        }

        // 2. AspectJ 표현식 추가
        if (aspectjExpr != null && !aspectjExpr.isBlank()) {
            pcs.add(new AspectJPointcutAdapter(aspectjExpr.trim()));
        }

        // 3. 조건이 없으면 예외
        if (pcs.isEmpty()) {
            throw new IllegalArgumentException("At least one of annotation[] or pointcut() must be provided.");
        }

        // 4. 단일 조건이면 직접 반환, 다중 조건이면 CompositePointcut으로 조합
        return pcs.size() == 1 ? pcs.get(0) : new CompositePointcut(pcs);
    }
}
```

**팩토리의 유연성**

1. **다중 어노테이션**: 여러 어노테이션 타입을 OR 조건으로 결합
2. **AspectJ 지원**: 복잡한 포인트컷 표현식 지원
3. **조합 전략**: 단일/다중 조건에 따른 최적의 포인트컷 생성
4. **입력 검증**: 최소한 하나의 조건은 반드시 제공되어야 함

## 초기화 및 생명주기

### 어드바이스 생성 과정

1. **@Aspect 클래스 스캔**: 컴포넌트 스캐닝으로 Aspect 빈 발견
2. **메서드 분석**: 각 메서드에서 @Before, @After, @Around 어노테이션 탐지
3. **AdviceType 결정**: 어노테이션 타입으로 적절한 AdviceType 선택
4. **AdviceBuilder 선택**: AdviceType에 맞는 빌더로 Advisor 생성
5. **AdvisorRegistry 등록**: 생성된 Advisor를 중앙 레지스트리에 등록

### 프록시 생성 시 활용

1. **타겟 클래스 분석**: 프록시 생성 대상 클래스와 메서드 분석
2. **적용 가능한 Advisor 검색**: AdvisorRegistry에서 매칭되는 Advisor들 조회
3. **인터셉터 체인 구성**: 매칭된 Advisor들의 Advice로 인터셉터 체인 구성
4. **순서 정렬**: Order 값에 따라 인터셉터 실행 순서 결정

## 성능 분석

### 시간 복잡도

**AdvisorRegistry 연산**
- **어드바이저 등록**: O(n log n) (정렬 포함)
- **적용 가능한 어드바이저 조회**:
  - 캐시 적중: O(1)
  - 캐시 미스: O(n) (n = 등록된 어드바이저 수)

**PointcutMatcher 연산**
- **AnnotationPointcut**: O(1) (어노테이션 존재 체크)
- **AspectJPointcutAdapter**: O(1) (AspectJ 내부 최적화)
- **CompositePointcut**: O(m) (m = 조합된 포인트컷 수)

### 메모리 사용 최적화

**캐싱 전략**

```java
// 메서드별 어드바이저 캐싱으로 반복 조회 비용 절약
private final Map<Method, List<Advisor>> cachedAdvisors = new ConcurrentHashMap<>();
```

**불변 객체 활용**

- **DefaultAdvisor**: 불변 객체로 안전한 공유
- **AdviceType**: 열거형으로 싱글톤 보장
- **Pointcut 구현체**: 상태 없는 매처로 재사용 가능

## Spring AOP와의 비교

### 아키텍처 차이점

| 특성 | Spring AOP | Sprout AOP |
|------|------------|------------|
| **Advice 인터페이스** | 타입별 다른 인터페이스 | 통일된 Advice 인터페이스 |
| **Pointcut 지원** | 다양한 Pointcut 타입 | Annotation + AspectJ |
| **어드바이저 등록** | BeanPostProcessor | 명시적 레지스트리 |
| **캐싱 전략** | ProxyFactory 레벨 | Method 레벨 캐싱 |
| **인터셉터 체인** | ReflectiveMethodInvocation | 커스텀 MethodInvocation |

### 설계 철학 차이

**Spring AOP**
- 다양한 Advice 타입별로 전용 인터페이스 제공
- 복잡한 ProxyFactory와 AdvisorChainFactory

**Sprout AOP**
- 단일 Advice 인터페이스로 단순화
- 명시적인 레지스트리와 팩토리 패턴

## 확장성과 커스터마이징

### 새로운 어드바이스 타입 추가

```java
// 1. 새로운 어드바이스 타입 정의
public enum AdviceType {
    // 기존 타입들...
    AFTER_RETURNING(AfterReturning.class),  // 신규 추가
}

// 2. 전용 빌더 구현
public class AfterReturningAdviceBuilder implements AdviceBuilder {
    @Override
    public Advisor build(Class<?> aspectCls, Method method,
                         Supplier<Object> aspectSup, PointcutFactory pf) {
        // 구현 로직
    }
}

// 3. AdviceFactory에 빌더 등록
this.builders = Map.of(
    // 기존 빌더들...
    AdviceType.AFTER_RETURNING, new AfterReturningAdviceBuilder()
);
```

### 커스텀 Pointcut 구현

```java
public class CustomPointcut implements Pointcut {
    @Override
    public boolean matches(Class<?> targetClass, Method method) {
        // 커스텀 매칭 로직
        return /* 조건 */;
    }
}
```

---

Sprout의 AOP Advice와 Advisor 시스템은 Spring AOP의 핵심 개념을 교육적 목적에 맞게 단순화하면서도, 실제 AOP의 동작 원리를 명확히 보여주는 구조로 설계되었습니다. 통일된 Advice 인터페이스, 명시적인 레지스트리 패턴, 그리고 효율적인 캐싱 전략을 통해 성능과 가독성을 모두 만족하는 구현을 제공합니다.

기여 및 개선 사항에 대한 제안은 언제나 환영합니다!