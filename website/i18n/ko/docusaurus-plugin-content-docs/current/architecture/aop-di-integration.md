# 🔗 AOP & DI/IoC Integration

## 개요

이 문서는 Sprout Framework에서 AOP(Aspect-Oriented Programming)가 DI/IoC 컨테이너와 어떻게 통합되어 자동 프록시 생성을 수행하는지에 대한 심층적인 기술 분석을 제공합니다. 인프라 빈의 초기화 순서부터 CGLIB 기반 프록시 생성, 그리고 메서드 인터셉션 체인까지의 전 과정을 상세히 분석하여 Sprout AOP의 완전한 작동 메커니즘을 이해할 수 있도록 합니다.

## 전체 아키텍처 개요

### AOP-DI 통합 흐름도

```
애플리케이션 시작
    ↓
SproutApplicationContext.refresh()
    ↓
1. 빈 정의 스캔 (scanBeanDefinitions)
    ├── @Component, @Service, @Repository 스캔
    ├── @Aspect 클래스 스캔
    └── InfrastructureBean vs ApplicationBean 분류
    ↓
2. 인프라 빈 초기화 (instantiateInfrastructureBeans)
    ├── AdvisorRegistry, AdviceFactory, ProxyFactory 생성
    ├── AspectPostProcessor 생성 및 등록
    └── PostInfrastructureInitializer 실행
    ↓
3. AopPostInfrastructureInitializer 실행
    ├── @Aspect 클래스 스캔
    ├── Advisor 생성 및 레지스트리 등록
    └── AspectPostProcessor 초기화
    ↓
4. BeanPostProcessor 등록 (registerBeanPostProcessors)
    └── AspectPostProcessor를 BeanPostProcessor로 등록
    ↓
5. 애플리케이션 빈 초기화 (instantiateAllSingletons)
    ├── 빈 생성 시 BeanPostProcessor 체인 실행
    ├── AspectPostProcessor.postProcessAfterInitialization 호출
    ├── 프록시 필요성 판단 및 CGLIB 프록시 생성
    └── BeanMethodInterceptor로 메서드 인터셉션 설정
```

### 핵심 설계 원칙

1. **인프라 우선 초기화**: AOP 관련 인프라 빈들이 애플리케이션 빈보다 먼저 초기화
2. **PostProcessor 패턴**: BeanPostProcessor를 통한 투명한 프록시 생성
3. **CGLIB 기반 프록시**: 인터페이스 없이도 프록시 생성 가능
4. **체인 오브 리스펀서빌리티**: 여러 어드바이스의 순차적 실행

## 인프라 빈 초기화 메커니즘

### 1. SproutApplicationContext의 초기화 전략

**단계별 초기화 과정**

```java
@Override
public void refresh() throws Exception {
    scanBeanDefinitions();           // 1. 빈 정의 스캔
    instantiateInfrastructureBeans(); // 2. 인프라 빈 초기화 (AOP 포함)
    instantiateAllSingletons();      // 3. 애플리케이션 빈 초기화

    // 4. 컨텍스트 후처리
    List<ContextInitializer> contextInitializers = getAllBeans(ContextInitializer.class);
    for (ContextInitializer initializer : contextInitializers) {
        initializer.initializeAfterRefresh(this);
    }
}
```

### 2. 빈 분류 전략: 인프라 vs 애플리케이션

**자동 분류 알고리즘**

```java
private void scanBeanDefinitions() throws NoSuchMethodException {
    // 모든 빈 정의 스캔
    Collection<BeanDefinition> allDefs = scanner.scan(configBuilder,
        Component.class, Controller.class, Service.class, Repository.class,
        Configuration.class, Aspect.class, ControllerAdvice.class, WebSocketHandler.class
    );

    // 인프라 빈 분류 (BeanPostProcessor + InfrastructureBean)
    List<BeanDefinition> infraDefs = new ArrayList<>(allDefs.stream()
        .filter(bd -> BeanPostProcessor.class.isAssignableFrom(bd.getType()) ||
                      InfrastructureBean.class.isAssignableFrom(bd.getType()))
        .toList());

    // 애플리케이션 빈 분류 (나머지)
    List<BeanDefinition> appDefs = new ArrayList<>(allDefs);
    appDefs.removeAll(infraDefs);

    this.infraDefs = infraDefs;
    this.appDefs = appDefs;
}
```

**분류 기준**

- **인프라 빈**: `BeanPostProcessor` 구현체 + `InfrastructureBean` 구현체
- **애플리케이션 빈**: 나머지 모든 빈 (비즈니스 로직 빈들)

**분류의 중요성**

1. **순서 보장**: AOP 인프라가 애플리케이션 빈보다 먼저 준비됨
2. **의존성 해결**: PostProcessor들이 애플리케이션 빈 생성 시점에 사용 가능
3. **초기화 분리**: 각 그룹별로 독립적인 초기화 전략 적용

### 3. PostInfrastructureInitializer 패턴

**인프라 빈 초기화 후 콜백**

```java
private void instantiateInfrastructureBeans() {
    instantiateGroup(infraDefs);  // 인프라 빈들 생성

    // PostInfrastructureInitializer 실행
    List<PostInfrastructureInitializer> initializers = beanFactory.getAllBeans(PostInfrastructureInitializer.class);
    for (PostInfrastructureInitializer initializer : initializers) {
        initializer.afterInfrastructureSetup(beanFactory, basePackages);
    }
}
```

**AopPostInfrastructureInitializer 구현**

```java
@Component
public class AopPostInfrastructureInitializer implements PostInfrastructureInitializer {
    private final AspectPostProcessor aspectPostProcessor;

    @Override
    public void afterInfrastructureSetup(BeanFactory beanFactory, List<String> basePackages) {
        aspectPostProcessor.initialize(basePackages);  // AspectPostProcessor 초기화
    }
}
```

**초기화 타이밍의 중요성**

- 모든 AOP 관련 인프라 빈(AdvisorRegistry, AdviceFactory 등)이 준비된 후 실행
- 애플리케이션 빈 생성 전에 모든 Advisor가 등록 완료
- BeanPostProcessor 등록 전에 AOP 설정 완료

## AspectPostProcessor: AOP의 핵심 엔진

### 1. 이중 역할 아키텍처

AspectPostProcessor는 두 가지 핵심 역할을 수행합니다:

1. **PostInfrastructureInitializer 시점**: Aspect 스캔 및 Advisor 등록
2. **BeanPostProcessor 시점**: 프록시 생성 여부 판단 및 실행

### 2. Aspect 스캔 및 Advisor 등록 과정

**초기화 메서드**

```java
public void initialize(List<String> basePackages) {
    if (initialized.compareAndSet(false, true)) {  // AtomicBoolean으로 중복 실행 방지
        this.basePackages = basePackages;
        scanAndRegisterAdvisors();
    }
}
```

**Reflections 라이브러리 기반 스캔**

```java
private void scanAndRegisterAdvisors() {
    // ConfigurationBuilder로 스캔 범위 설정
    ConfigurationBuilder configBuilder = new ConfigurationBuilder();
    for (String pkg : basePackages) {
        configBuilder.addUrls(ClasspathHelper.forPackage(pkg));
    }
    configBuilder.addScanners(Scanners.TypesAnnotated, Scanners.SubTypes);

    // 패키지 필터링
    FilterBuilder filter = new FilterBuilder();
    for (String pkg : basePackages) {
        filter.includePackage(pkg);
    }
    configBuilder.filterInputsBy(filter);

    // @Aspect 어노테이션이 붙은 클래스 검색
    Reflections reflections = new Reflections(configBuilder);
    Set<Class<?>> aspectClasses = reflections.getTypesAnnotatedWith(Aspect.class);

    // 각 Aspect 클래스에서 Advisor 생성 및 등록
    for (Class<?> aspectClass : aspectClasses) {
        List<Advisor> advisorsForThisAspect = createAdvisorsFromAspect(aspectClass);
        for (Advisor advisor : advisorsForThisAspect) {
            advisorRegistry.registerAdvisor(advisor);
        }
    }
}
```

**Aspect에서 Advisor 생성**

```java
private List<Advisor> createAdvisorsFromAspect(Class<?> aspectClass) {
    List<Advisor> advisors = new ArrayList<>();

    // ApplicationContext에서 빈 조회를 위한 Supplier
    Supplier<Object> aspectSupplier = () -> container.getBean(aspectClass);

    // 모든 메서드를 순회하며 어드바이스 어노테이션 확인
    for (Method m : aspectClass.getDeclaredMethods()) {
        adviceFactory.createAdvisor(aspectClass, m, aspectSupplier)
                .ifPresent(advisors::add);
    }

    return advisors;
}
```

### 3. BeanPostProcessor로서의 프록시 생성

**후처리 메서드**

```java
@Override
public Object postProcessAfterInitialization(String beanName, Object bean) {
    Class<?> targetClass = bean.getClass();

    // 프록시 필요성 판단
    boolean needsProxy = false;
    for (Method method : targetClass.getMethods()) {
        if (Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())) {
            if (!advisorRegistry.getApplicableAdvisors(targetClass, method).isEmpty()) {
                needsProxy = true;
                break;
            }
        }
    }

    // 프록시 생성 및 반환
    if (needsProxy) {
        CtorMeta meta = container.lookupCtorMeta(bean);
        return proxyFactory.createProxy(targetClass, bean, advisorRegistry, meta);
    }

    return bean;  // 프록시 불필요 시 원본 반환
}
```

**프록시 필요성 판단 최적화**

1. **public 메서드만 검사**: private/protected 메서드는 AOP 적용 대상 아님
2. **static 메서드 제외**: 인스턴스 메서드만 인터셉션 가능
3. **조기 종료**: 하나라도 적용 가능한 Advisor 발견 시 즉시 프록시 생성
4. **캐시 활용**: AdvisorRegistry의 메서드별 캐싱 활용

## CGLIB 기반 프록시 생성 시스템

### 1. CglibProxyFactory: 프록시 생성 전문가

**간결한 프록시 생성**

```java
@Component
public class CglibProxyFactory implements ProxyFactory, InfrastructureBean {
    @Override
    public Object createProxy(Class<?> targetClass, Object target, AdvisorRegistry registry, CtorMeta meta) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(targetClass);          // 상속 기반 프록시
        enhancer.setCallback(new BeanMethodInterceptor(target, registry));  // 메서드 인터셉터 설정
        return enhancer.create(meta.paramTypes(), meta.args());  // 생성자 파라미터로 인스턴스 생성
    }
}
```

**CGLIB Enhancer 설정**

1. **setSuperclass**: 원본 클래스를 부모 클래스로 설정 (상속 기반 프록시)
2. **setCallback**: 모든 메서드 호출을 인터셉트할 콜백 설정
3. **create**: 원본 객체와 동일한 생성자 파라미터로 프록시 인스턴스 생성

**CtorMeta 활용**

- 원본 빈 생성 시 사용된 생성자 정보를 보존
- 프록시 생성 시 동일한 생성자 파라미터 사용
- DI 컨테이너의 생성 일관성 보장

### 2. BeanMethodInterceptor: 메서드 인터셉션 허브

**CGLIB MethodInterceptor 구현**

```java
public class BeanMethodInterceptor implements MethodInterceptor {
    private final Object target;                    // 원본 객체
    private final AdvisorRegistry advisorRegistry;  // 어드바이저 레지스트리

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        // 적용 가능한 어드바이저 조회 (캐시 활용)
        List<Advisor> applicableAdvisors = advisorRegistry.getApplicableAdvisors(target.getClass(), method);

        if (applicableAdvisors.isEmpty()) {
            // 어드바이저 없으면 원본 메서드 직접 호출
            return proxy.invoke(target, args);
        }

        // 어드바이스 체인 실행을 위한 MethodInvocation 생성
        MethodInvocationImpl invocation = new MethodInvocationImpl(target, method, args, proxy, applicableAdvisors);
        return invocation.proceed();
    }
}
```

**인터셉션 최적화 전략**

1. **조기 분기**: 적용할 어드바이저가 없으면 즉시 원본 메서드 호출
2. **캐시 활용**: AdvisorRegistry의 메서드별 어드바이저 캐싱
3. **지연 생성**: MethodInvocation은 필요한 경우에만 생성
4. **직접 호출**: CGLIB의 MethodProxy.invoke()로 성능 최적화


## 프록시 전략: 위임형 vs 단일 인스턴스형

Sprout AOP에서 프록시 생성은 크게 두 가지 전략으로 나뉩니다.

### 1. 위임형(Delegating Proxy)

- **구조**: 원본 인스턴스를 먼저 생성하고, 프록시는 단순히 호출을 위임
- **인터셉터 동작**: `proxy.invoke(target, args)`
- **특징**:
    - 원본과 프록시가 모두 존재
    - 원본의 생성자 부작용이 두 번 발생할 수 있음 (원본 생성 + 프록시 생성)
    - Objenesis를 사용하여 프록시 생성자의 실행을 건너뛰어 “2중 생성” 문제를 방지
- **사용 시점**: 원본 인스턴스의 상태나 생성자 로직을 반드시 살려야 할 때

### 2. 단일 인스턴스형(Subclassing Proxy)

- **구조**: CGLIB이 원본 클래스를 상속한 서브클래스를 생성, 이것이 곧 빈
- **인터셉터 동작**: `proxy.invokeSuper(this, args)`
- **특징**:
    - 별도의 원본 인스턴스는 없음
    - 프록시 생성 시 선택된 생성자를 한 번만 호출하므로 “2중 생성” 문제가 발생하지 않음
    - DI는 프록시 인스턴스 자체에 수행됨 (생성자/필드/세터 모두 프록시가 대상)
- **사용 시점**: 프록시가 곧 빈 역할을 하고, 원본 객체를 따로 관리할 필요가 없을 때

### Sprout의 선택

Sprout은 **단일 인스턴스형** 전략을 기본으로 채택했습니다.

이는 구조적으로 단순하고, “생성자 2번 호출” 문제를 제거하며, DI 컨테이너와도 자연스럽게 통합됩니다.

즉:

- **Aspect 클래스**는 일반 빈으로 DI 완료 후 레지스트리에 등록
- **애플리케이션 빈**은 프록시 인스턴스로 생성자 DI를 한 번만 수행
- 순환 참조가 발생하면 `getBean()` 재진입을 통해 해결

이를 통해 개발자는 프록시 존재 여부에 신경 쓰지 않고, 평범한 빈처럼 의존성을 주입받고 사용할 수 있습니다.

## Objenesis Fallback: 래핑 AOP 지원 전략

Sprout은 기본적으로 **단일 인스턴스형(Subclassing Proxy)** 모델을 채택합니다. 그러나 향후 **래핑(Delegating) AOP**를 지원해야 할 경우, 별도의 **Objenesis 기반 fallback 경로**가 필요합니다.

### 왜 Objenesis가 필요한가

- 위임형에서는 프록시 생성 시 원본 인스턴스를 이미 갖고 있음
- 만약 `enhancer.create(..)`를 그대로 사용하면:
    - 프록시 인스턴스 생성 과정에서 슈퍼 생성자가 다시 호출
    - 결과적으로 원본 생성자 로직이 **2번 실행**됨 (원본 + 프록시)
- 이는 부작용 발생, final 필드 재할당, 리소스 이중 초기화 문제를 야기할 수 있음
- 따라서 **생성자 호출을 건너뛰고 빈 인스턴스를 만드는 기술**이 필요 → Objenesis 활용

### Fallback 경로 예시

```java
@Component
public class CglibProxyFactory implements ProxyFactory, InfrastructureBean {

    @Override
    public Object createProxy(Class<?> targetClass, Object target, AdvisorRegistry registry, CtorMeta meta) {
        Enhancer e = new Enhancer();
        e.setSuperclass(targetClass);

        if (target != null) {
            //  Delegating Proxy 경로: 이미 target이 존재 → Objenesis로 ctor skip
            e.setCallbackType(MethodInterceptor.class);
            Class<?> proxyClass = e.createClass();
            Object proxy = objenesis.newInstance(proxyClass);   // 생성자 호출 생략
            ((Factory) proxy).setCallback(0, new BeanMethodInterceptor(target, registry));
            return proxy;
        } else {
            //  Subclassing Proxy 경로: 프록시가 곧 빈 → ctor 정상 호출
            e.setCallback(new BeanMethodInterceptor(null, registry));
            return e.create(meta.paramTypes(), meta.args());
        }
    }
}

```

### 전략 요약

- **SubClassing(단일 인스턴스형)**: 기본 경로. 프록시 = 빈, ctor 정상 호출, 주입 그대로 반영.
- **Delegating(래핑형)**: Fallback 경로. 원본 별도 존재 → 프록시는 Objenesis로 생성, ctor 생략.

### 적용 시 고려사항

1. **DI 일관성**: Delegating 모델에서는 원본 객체에 DI가 이미 완료되어야 함. 프록시는 단순 위임자.
2. **캐싱 전략**: `(targetClass, advisorsSignature)`를 키로 프록시 클래스를 캐싱, Objenesis 인스턴스화 비용 최소화.
3. **순환 참조 처리**: 원본과 Aspect가 서로 참조하는 경우, 컨테이너의 `getBean()` 재진입 구조로 해결 가능.
4. **테스트 권장 시나리오**:
    - 원본 생성자 부작용이 1회만 발생하는지
    - final 필드나 리소스 핸들러가 안전하게 유지되는지
    - Delegating/Subclassing 두 경로가 동시에 섞여도 문제없는지

## MethodInvocation 체인 실행 시스템

### 1. MethodInvocationImpl: 체인 오브 리스펀서빌리티 구현

**어드바이스 체인 상태 관리**

```java
public class MethodInvocationImpl implements MethodInvocation {
    private final Object target;                    // 원본 객체
    private final Method method;                    // 호출될 메서드
    private final Object[] args;                    // 메서드 인자
    private final MethodProxy methodProxy;          // CGLIB 메서드 프록시
    private final List<Advisor> advisors;          // 적용할 어드바이저 목록
    private int currentAdvisorIndex = -1;          // 현재 실행 중인 어드바이저 인덱스

    @Override
    public Object proceed() throws Throwable {
        currentAdvisorIndex++;  // 다음 어드바이저로 이동

        if (currentAdvisorIndex < advisors.size()) {
            // 다음 어드바이저의 Advice 실행
            Advisor advisor = advisors.get(currentAdvisorIndex);
            return advisor.getAdvice().invoke(this);  // 재귀적 체인 실행
        } else {
            // 모든 어드바이저 실행 완료 → 원본 메서드 호출
            return methodProxy.invoke(target, args);
        }
    }
}
```

**체인 실행 흐름**

```
proceed() 호출
    ↓
currentAdvisorIndex++
    ↓
index < advisors.size() ?
    ├─ Yes → advisor.getAdvice().invoke(this) → 어드바이스 실행
    │                                              ↓
    │                                         proceed() 재귀 호출
    │                                              ↓
    │                                         다음 어드바이저 또는 원본 메서드
    └─ No → methodProxy.invoke(target, args) → 원본 메서드 실행
```

### 2. MethodSignature: 메서드 메타데이터 최적화

**지연 계산과 캐싱 전략**

```java
public class MethodSignature implements Signature {
    private final Method method;
    private volatile String cachedToString;      // 문자열 표현 캐싱
    private volatile String cachedLongName;      // 긴 이름 캐싱

    @Override
    public String toLongName() {
        String local = cachedLongName;
        if (local == null) {                          // 첫 호출 시 null
            synchronized (this) {                     // 동기화 블록
                if (cachedLongName == null) {         // double-checked locking
                    cachedLongName = method.toGenericString();
                }
                local = cachedLongName;
            }
        }
        return local;
    }
}
```

**성능 최적화 기법**

1. **Volatile 필드**: 메모리 가시성 보장
2. **Double-Checked Locking**: 동기화 비용 최소화
3. **지연 초기화**: 실제 사용 시점에만 계산
4. **로컬 변수 활용**: 중복 volatile 읽기 방지

## DI 컨테이너와의 통합 메커니즘

### 1. BeanPostProcessor 등록 시점

**등록 전략**

```java
private void registerBeanPostProcessors() {
    List<BeanPostProcessor> allBeanPostProcessor = beanFactory.getAllBeans(BeanPostProcessor.class);

    for (BeanPostProcessor beanPostProcessor : allBeanPostProcessor) {
        beanFactory.addBeanPostProcessor(beanPostProcessor);
    }
}
```

**실행 시점**: 인프라 빈 초기화 완료 후, 애플리케이션 빈 초기화 직전

### 2. 빈 생성 라이프사이클과 AOP 통합

**빈 생성 과정에서의 AOP 개입**

```java
// DefaultListableBeanFactory 내부의 빈 생성 과정
public Object createBean(BeanDefinition bd) {
    // 1. 인스턴스 생성
    Object instance = instantiateBean(bd);

    // 2. 의존성 주입
    injectDependencies(instance, bd);

    // 3. BeanPostProcessor 실행 (AOP 포함)
    for (BeanPostProcessor processor : beanPostProcessors) {
        instance = processor.postProcessAfterInitialization(bd.getName(), instance);
    }

    return instance;
}
```

### 3. 프록시와 원본 객체 메타데이터 보존

**CtorMeta 활용**

```java
// 원본 빈 생성 시 생성자 정보 저장
private final Map<Object, CtorMeta> ctorCache = new IdentityHashMap<>();

// 프록시 생성 시 동일한 생성자 정보 사용
CtorMeta meta = container.lookupCtorMeta(bean);
return proxyFactory.createProxy(targetClass, bean, advisorRegistry, meta);
```

## 성능 분석 및 최적화

### 1. 시간 복잡도 분석

**프록시 생성 결정 과정**
- **메서드 순회**: O(m) (m = 클래스의 public 메서드 수)
- **어드바이저 매칭**: O(n) × O(p) (n = 어드바이저 수, p = 포인트컷 매칭 복잡도)
- **캐시 적중 시**: O(1) (AdvisorRegistry 캐싱 활용)

**메서드 인터셉션 과정**
- **어드바이저 조회**: O(1) (캐시 적중 시)
- **체인 실행**: O(a) (a = 적용 가능한 어드바이저 수)
- **원본 메서드 호출**: O(1) (CGLIB MethodProxy 직접 호출)

### 2. 메모리 사용 최적화

**캐싱 전략**

```java
// AdvisorRegistry에서 메서드별 캐싱
private final Map<Method, List<Advisor>> cachedAdvisors = new ConcurrentHashMap<>();

// MethodSignature에서 문자열 표현 캐싱
private volatile String cachedToString;
private volatile String cachedLongName;
```

**메모리 효율성**

1. **ConcurrentHashMap**: 읽기 중심 최적화
2. **IdentityHashMap**: 객체 동일성 기반 빠른 조회
3. **AtomicBoolean**: 초기화 중복 방지
4. **Volatile 캐싱**: 지연 초기화와 메모리 가시성

### 3. CGLIB vs JDK 동적 프록시 비교

| 특성 | CGLIB | JDK 동적 프록시 |
|------|-------|----------------|
| **기반 기술** | 바이트코드 생성 | 리플렉션 |
| **인터페이스 요구** | 불필요 | 필수 |
| **상속 기반** | 클래스 상속 | 인터페이스 구현 |
| **성능** | 빠름 (직접 호출) | 느림 (리플렉션) |
| **final 메서드** | 인터셉트 불가 | 해당 없음 |
| **생성자 지원** | 지원 | 미지원 |

**Sprout이 CGLIB를 선택한 이유**

1. **인터페이스 독립성**: 비즈니스 클래스에 인터페이스 강제 불필요
2. **성능 우선**: MethodProxy를 통한 직접 호출로 성능 최적화
3. **생성자 지원**: DI와 자연스러운 통합

## Spring AOP와의 비교

### 아키텍처 차이점

| 측면 | Spring AOP | Sprout AOP |
|------|------------|------------|
| **프록시 생성 시점** | BeanPostProcessor | BeanPostProcessor |
| **인프라 초기화** | BeanFactoryPostProcessor | PostInfrastructureInitializer |
| **Aspect 스캔** | 컴포넌트 스캔 통합 | 별도 Reflections 스캔 |
| **어드바이저 등록** | 자동 + BeanDefinition | 명시적 레지스트리 |
| **프록시 팩토리** | ProxyFactory (복잡) | CglibProxyFactory (단순) |
| **메서드 체인** | ReflectiveMethodInvocation | MethodInvocationImpl |

### 설계 철학 차이

**Spring AOP**
- 복잡하고 유연한 프록시 생성 전략
- 다양한 프록시 타입 지원 (JDK + CGLIB)
- BeanDefinition 기반 메타데이터 관리

**Sprout AOP**
- 단순하고 명확한 프록시 생성 전략
- CGLIB만 지원하여 복잡성 제거
- 명시적 레지스트리 패턴으로 가독성 향상

## 확장성과 커스터마이징

### 1. 새로운 ProxyFactory 구현

```java
@Component
public class CustomProxyFactory implements ProxyFactory, InfrastructureBean {
    @Override
    public Object createProxy(Class<?> targetClass, Object target, AdvisorRegistry registry, CtorMeta meta) {
        // JDK 동적 프록시 또는 다른 프록시 기술 사용
        return createCustomProxy(targetClass, target, registry);
    }
}
```

### 2. 커스텀 PostInfrastructureInitializer

```java
@Component
public class CustomAopInitializer implements PostInfrastructureInitializer {
    @Override
    public void afterInfrastructureSetup(BeanFactory beanFactory, List<String> basePackages) {
        // 커스텀 AOP 초기화 로직
        initializeCustomAspects();
    }
}
```

### 3. BeanPostProcessor 체인 확장

```java
@Component
@Order(100)  // AspectPostProcessor 이후 실행
public class CustomPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(String beanName, Object bean) {
        // 추가적인 후처리 로직
        return enhanceBean(bean);
    }
}
```

## 디버깅과 모니터링

### 1. AOP 적용 여부 확인

```java
// AspectPostProcessor에서 프록시 생성 시 로깅
if (needsProxy) {
    System.out.println("Applying AOP proxy to bean: " + beanName + " (" + targetClass.getName() + ")");
    // ...
}
```

### 2. Advisor 등록 현황 추적

```java
// AspectPostProcessor에서 Advisor 등록 후 로깅
System.out.println(aspectClass.getName() + " has " + advisorsForThisAspect.size() + " advisors: " + advisorsForThisAspect);
System.out.println("advisorRegistry#getAllAdvisors()" + advisorRegistry.getAllAdvisors());
```

### 3. 메서드 인터셉션 모니터링

```java
// BeanMethodInterceptor에서 인터셉션 발생 시 로깅
if (!applicableAdvisors.isEmpty()) {
    System.out.println("Intercepting method: " + method.getName() + " with " + applicableAdvisors.size() + " advisors");
}
```

## 보안 고려사항

### 1. CGLIB 기반 프록시의 제한사항

**보안 제약**
- **final 클래스**: CGLIB로 프록시 생성 불가
- **final 메서드**: 오버라이드 불가로 인터셉션 불가
- **private 메서드**: 프록시에서 접근 불가
- **생성자 호출**: 원본 객체의 생성자가 두 번 호출됨

### 2. 권한 검증 강화

```java
// AspectPostProcessor에서 프록시 생성 전 권한 검증
if (needsProxy && !hasProxyPermission(targetClass)) {
    throw new SecurityException("Proxy creation not allowed for: " + targetClass.getName());
}
```

---

Sprout의 AOP와 DI/IoC 통합 시스템은 Spring의 복잡한 프록시 생성 메커니즘을 교육적 목적에 맞게 단순화하면서도, 실제 AOP의 핵심 원리를 명확히 보여주는 구조로 설계되었습니다.

인프라 빈의 우선 초기화, PostInfrastructureInitializer 패턴, BeanPostProcessor 체인, 그리고 CGLIB 기반 프록시 생성을 통해 투명하고 효율적인 AOP 통합을 제공합니다.

확장성과 디버깅 편의성을 고려한 설계로 개발자들이 AOP의 내부 동작을 쉽게 이해하고 커스터마이징할 수 있도록 합니다.