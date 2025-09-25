# 🏗️ IoC Container

IoC(Inversion of Control) 컨테이너는 Sprout Framework의 핵심입니다. 애플리케이션의 모든 컴포넌트에 대한 객체 생성, 의존성 주입, 생명주기를 관리합니다.

## 개요

Sprout의 IoC 컨테이너는 다음 기능을 제공합니다:
- **컴포넌트 스캔**: Reflections 라이브러리를 이용한 어노테이션 기반 클래스 자동 감지
- **생성자 주입**: 타입 안전한 의존성 해결 (필드 주입 미지원)
- **생명주기 관리**: 빈 생성, 초기화, 소멸
- **순환 의존성 감지**: BeanGraph를 통한 위상 정렬과 순환 참조 감지
- **순서 지원**: @Order를 통한 빈 초기화 및 컬렉션 순서 제어
- **CGLIB 프록시**: @Configuration 클래스의 싱글톤 보장

## 컨테이너 아키텍처

### 핵심 컴포넌트

Sprout의 IoC 컨테이너는 다음 주요 클래스들로 구성됩니다.

- `SproutApplicationContext`: 메인 애플리케이션 컨텍스트
- `DefaultListableBeanFactory`: 핵심 빈 팩토리 구현
- `ClassPathScanner`: 클래스패스 스캔 및 빈 정의 생성
- `BeanGraph`: 의존성 그래프와 위상 정렬

### 컨테이너 초기화 과정

```java
public class SproutApplication {
    public static void run(Class<?> primarySource) throws Exception {
        // 1. 패키지 스캔 설정
        List<String> packages = getPackagesToScan(primarySource);
        
        // 2. 애플리케이션 컨텍스트 생성
        ApplicationContext applicationContext = 
            new SproutApplicationContext(packages.toArray(new String[0]));
        
        // 3. 컨텍스트 초기화 (refresh)
        applicationContext.refresh();
        
        // 4. 서버 시작
        HttpServer server = applicationContext.getBean(HttpServer.class);
        server.start(port);
    }
}
```

## 컴포넌트 스캔

### 지원되는 어노테이션

Sprout는 다음 컴포넌트 어노테이션을 인식합니다:

```java
@Component         // 일반 컴포넌트
@Service          // 비즈니스 로직 계층
@Repository       // 데이터 접근 계층
@Controller       // 웹 계층
@Configuration    // 구성 클래스
@Aspect           // AOP 애스펙트
@ControllerAdvice // 글로벌 예외 처리
@WebSocketHandler // WebSocket 핸들러
```

### 스캔 과정

```java
// ClassPathScanner의 스캔 로직
public Collection<BeanDefinition> scan(ConfigurationBuilder configBuilder, 
                                     Class<? extends Annotation>... componentAnnotations) {
    // 1. Reflections를 이용한 어노테이션 기반 클래스 탐색
    Set<Class<?>> componentCandidates = new HashSet<>();
    for (Class<? extends Annotation> anno : componentAnnotations) {
        componentCandidates.addAll(r.getTypesAnnotatedWith(anno));
    }
    
    // 2. 구체 클래스만 필터링 (인터페이스, 추상클래스 제외)
    Set<Class<?>> concreteComponentTypes = componentCandidates.stream()
        .filter(clazz -> !clazz.isInterface() && 
                        !clazz.isAnnotation() && 
                        !Modifier.isAbstract(clazz.getModifiers()))
        .collect(Collectors.toSet());
    
    // 3. @Bean 메서드 기반 빈 탐색
    Set<Class<?>> configClasses = r.getTypesAnnotatedWith(Configuration.class);
    for (Class<?> configClass : configClasses) {
        for (Method method : configClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Bean.class)) {
                beanMethodReturnTypes.add(method.getReturnType());
            }
        }
    }
}
```

### 컴포넌트 스캔 활성화

메인 애플리케이션 클래스에 `@ComponentScan`을 사용하세요:

```java
@ComponentScan("com.myapp")  // 특정 패키지 스캔
@ComponentScan({"com.myapp.web", "com.myapp.service"})  // 여러 패키지
public class Application {
    public static void main(String[] args) throws Exception {
        SproutApplication.run(Application.class);
    }
}
```

## 의존성 주입

### 생성자 주입 전략

Sprout는 **생성자 주입만**을 지원합니다. 가장 많은 매개변수를 가진 해결 가능한 생성자를 선택합니다.

```java
// 생성자 해결 로직
private Constructor<?> resolveUsableConstructor(Class<?> clazz, Set<Class<?>> allKnownBeanTypes) {
    return Arrays.stream(clazz.getDeclaredConstructors())
        .filter(constructor -> Arrays.stream(constructor.getParameterTypes())
            .allMatch(param -> isResolvable(param, allKnownBeanTypes)))
        .max(Comparator.comparingInt(Constructor::getParameterCount))
        .orElseThrow(() -> new NoSuchMethodException("No usable constructor"));
}
```

### 의존성 해결 규칙

```java
private boolean isResolvable(Class<?> paramType, Set<Class<?>> allKnownBeanTypes) {
    // 1. List 타입은 항상 해결 가능
    if (List.class.isAssignableFrom(paramType)) {
        return true;
    }
    
    // 2. 인프라 타입 확인 (ApplicationContext, BeanFactory 등)
    if (isKnownInfrastructureType(paramType)) {
        return true;
    }
    
    // 3. 알려진 빈 타입 중에서 할당 가능한 타입 찾기
    return allKnownBeanTypes.stream()
        .anyMatch(knownType -> paramType.isAssignableFrom(knownType));
}
```

### 예제: 기본 의존성 주입

```java
@Service
public class UserService {
    private final UserRepository repository;
    private final EmailService emailService;

    // 생성자 주입 - @Autowired 불필요
    public UserService(UserRepository repository, EmailService emailService) {
        this.repository = repository;
        this.emailService = emailService;
    }
}

@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
}
```

### 컬렉션 주입

특정 타입의 모든 빈을 `List`로 주입받을 수 있습니다:

```java
public interface EventHandler {
    void handle(Event event);
}

@Component
@Order(1)
public class EmailEventHandler implements EventHandler {
    public void handle(Event event) { /* 이메일 처리 */ }
}

@Component
@Order(2)
public class LogEventHandler implements EventHandler {
    public void handle(Event event) { /* 로그 처리 */ }
}

@Service
public class EventProcessor {
    private final List<EventHandler> handlers;

    // 모든 EventHandler 빈이 @Order 순서대로 주입됨
    public EventProcessor(List<EventHandler> handlers) {
        this.handlers = handlers;
    }
    
    public void processEvent(Event event) {
        handlers.forEach(handler -> handler.handle(event));
    }
}
```

### 컬렉션 주입 처리 로직

```java
// DefaultListableBeanFactory의 컬렉션 주입 후처리
protected void postProcessListInjections() {
    for (PendingListInjection pending : pendingListInjections) {
        Set<Object> uniqueBeansForList = new HashSet<>();
        for (Object bean : singletons.values()) {
            if (pending.getGenericType().isAssignableFrom(bean.getClass())) {
                uniqueBeansForList.add(bean);
            }
        }

        // @Order 어노테이션에 따라 정렬
        List<Object> sortedBeansForList = uniqueBeansForList.stream()
            .sorted(Comparator.comparingInt(bean -> {
                Class<?> clazz = bean.getClass();
                Order order = clazz.getAnnotation(Order.class);
                return (order != null) ? order.value() : Integer.MAX_VALUE;
            }))
            .toList();

        pending.getListToPopulate().clear();
        pending.getListToPopulate().addAll(sortedBeansForList);
    }
}
```

## 빈 정의와 생성

### 빈 정의 타입

Sprout는 두 가지 빈 생성 방식을 지원합니다.

1. **생성자 기반 빈** (`ConstructorBeanDefinition`)
2. **팩토리 메서드 빈** (`MethodBeanDefinition`)

### 생성자 기반 빈

```java
@Component
public class NotificationService {
    private final EmailService emailService;
    private final SmsService smsService;

    public NotificationService(EmailService emailService, SmsService smsService) {
        this.emailService = emailService;
        this.smsService = smsService;
    }
}
```

### 팩토리 메서드 빈

```java
@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/myapp");
        return new HikariDataSource(config);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
```

### @Configuration 프록시

`@Configuration` 클래스는 CGLIB을 이용해 프록시를 생성하여 싱글톤을 보장합니다.

```java
@Configuration(proxyBeanMethods = true)  // 기본값
public class AppConfig {
    @Bean
    public ServiceA serviceA() {
        return new ServiceA(serviceB());  // 동일한 serviceB 인스턴스 반환
    }

    @Bean
    public ServiceB serviceB() {
        return new ServiceB();
    }
}
```

```java
// 프록시 생성 로직
if (def.isConfigurationClassProxyNeeded()) {
    Enhancer enhancer = new Enhancer();
    enhancer.setSuperclass(def.getType());
    enhancer.setCallback(new ConfigurationMethodInterceptor(this));
    beanInstance = enhancer.create(def.getConstructorArgumentTypes(), deps);
}
```

## 생명주기 관리

### 컨테이너 초기화 과정

```java
@Override
public void refresh() throws Exception {
    // 1. 빈 정의 스캔
    scanBeanDefinitions();
    
    // 2. 인프라 빈 먼저 생성 (BeanPostProcessor 등)
    instantiateInfrastructureBeans();
    
    // 3. 애플리케이션 빈 생성
    instantiateAllSingletons();
    
    // 4. 컨텍스트 초기화 완료 후 콜백
    List<ContextInitializer> contextInitializers = getAllBeans(ContextInitializer.class);
    for (ContextInitializer initializer : contextInitializers) {
        initializer.initializeAfterRefresh(this);
    }
}
```

### 빈 생성 순서

Sprout는 `BeanGraph`를 이용해 의존성 그래프를 분석하고 위상 정렬하여 올바른 순서로 빈을 생성합니다.

```java
private void instantiateGroup(List<BeanDefinition> defs) {
    // 위상 정렬로 의존성 순서 결정
    List<BeanDefinition> order = new BeanGraph(defs).topologicallySorted();
    
    // 순서대로 빈 생성
    order.forEach(beanFactory::createBean);
    
    // 컬렉션 주입 후처리
    beanFactory.postProcessListInjections();
}
```

### @Primary와 빈 선택

동일한 타입의 빈이 여러 개일 때 `@Primary`로 우선순위를 지정할 수 있습니다.

```java
private String choosePrimary(Class<?> requiredType, Set<String> candidates) {
    // 1. @Primary가 붙은 빈 찾기
    List<String> primaries = candidates.stream()
        .filter(name -> {
            BeanDefinition d = beanDefinitions.get(name);
            return d != null && d.isPrimary();
        })
        .toList();

    if (primaries.size() == 1) return primaries.get(0);
    if (primaries.size() > 1)
        throw new RuntimeException("@Primary beans conflict for type " + requiredType.getName());

    return null;
}
```

### 빈 후처리 (Bean Post Processing)

```java
// 빈 생성 후 BeanPostProcessor 적용
Object processedBean = beanInstance;
for (BeanPostProcessor processor : beanPostProcessors) {
    Object result = processor.postProcessBeforeInitialization(def.getName(), processedBean);
    if (result != null) processedBean = result;
}
for (BeanPostProcessor processor : beanPostProcessors) {
    Object result = processor.postProcessAfterInitialization(def.getName(), processedBean);
    if (result != null) processedBean = result;
}
```

## 순환 의존성 감지

Sprout는 시작 시 `BeanGraph`를 통해 순환 의존성을 감지합니다. 순환 참조가 발견되면 예외를 발생시켜 애플리케이션 시작을 중단합니다.

```java
@Component
public class ServiceA {
    public ServiceA(ServiceB serviceB) { /* ... */ }
}

@Component
public class ServiceB {
    public ServiceB(ServiceC serviceC) { /* ... */ }
}

@Component
public class ServiceC {
    public ServiceC(ServiceA serviceA) { /* ... */ }  // 순환 의존성!
}

// 위상 정렬 시 순환 의존성이 감지되어 시작 오류 발생
```

## 빈 등록과 검색

### 타입별 빈 매핑

```java
// 타입별 빈 이름 매핑 (인터페이스, 상위클래스 포함)
private void registerInternal(String name, Object bean) {
    singletons.put(name, bean);

    Class<?> type = bean.getClass();
    primaryTypeToNameMap.putIfAbsent(type, name);
    typeToNamesMap.computeIfAbsent(type, k -> new HashSet<>()).add(name);

    // 인터페이스 등록
    for (Class<?> iface : type.getInterfaces()) {
        primaryTypeToNameMap.putIfAbsent(iface, name);
        typeToNamesMap.computeIfAbsent(iface, k -> new HashSet<>()).add(name);
    }
    
    // 상위 클래스 등록
    for (Class<?> p = type.getSuperclass(); 
         p != null && p != Object.class; 
         p = p.getSuperclass()) {
        primaryTypeToNameMap.putIfAbsent(p, name);
        typeToNamesMap.computeIfAbsent(p, k -> new HashSet<>()).add(name);
    }
}
```

### 빈 검색

```java
@Override
public <T> T getBean(Class<T> requiredType) {
    // 1. 이미 생성된 빈이 있는지 확인
    T bean = getIfPresent(requiredType);
    if (bean != null) return bean;

    // 2. 후보 수집
    Set<String> candidates = candidateNamesForType(requiredType);
    if (candidates.isEmpty()) {
        throw new RuntimeException("No bean of type " + requiredType.getName() + " found");
    }

    // 3. Primary 선택
    String primary = choosePrimary(requiredType, candidates);
    if (primary == null) {
        if (candidates.size() == 1) primary = candidates.iterator().next();
        else throw new RuntimeException("No unique bean of type " + requiredType.getName());
    }

    // 4. 필요시 생성 후 반환
    return (T) createIfNecessary(primary);
}
```

## 모범 사례

### 1. 생성자 주입 사용
```java
// 권장: 생성자 주입으로 불변성 보장
@Service
public class UserService {
    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }
}
```

### 2. 인터페이스 기반 설계
```java
// 권장: 인터페이스에 의존
@Service
public class OrderService {
    private final PaymentProcessor paymentProcessor;

    public OrderService(PaymentProcessor paymentProcessor) {
        this.paymentProcessor = paymentProcessor;
    }
}

@Component
public class StripePaymentProcessor implements PaymentProcessor {
    // 구현
}
```

### 3. 순환 의존성 방지
```java
// 권장: 이벤트로 순환 끊기
@Service
public class OrderService {
    private final EventPublisher eventPublisher;

    public void processOrder(Order order) {
        // 주문 처리
        eventPublisher.publish(new OrderProcessedEvent(order));
    }
}

@Component
public class InventoryService {
    @EventListener
    public void handleOrderProcessed(OrderProcessedEvent event) {
        // 재고 업데이트
    }
}
```

### 4. @Order로 순서 제어
```java
@Component
@Order(1)
public class ValidationFilter implements Filter {
    // 먼저 실행
}

@Component
@Order(2)
public class AuthenticationFilter implements Filter {
    // 검증 후 실행
}
```

## 성능 최적화

### 지연 로딩과 즉시 로딩

Sprout는 기본적으로 모든 싱글톤 빈을 애플리케이션 시작 시 생성합니다. 이는 다음과 같은 장점을 제공합니다.

- 시작 시 설정 오류 조기 발견
- 런타임 성능 향상
- 메모리 사용량 예측 가능

### 빈 스코프

현재 Sprout는 싱글톤 스코프만 지원하며, 모든 빈은 애플리케이션 전체에서 하나의 인스턴스만 생성됩니다.

## 확장 포인트

### BeanDefinitionRegistrar

사용자 정의 빈 정의를 동적으로 등록할 수 있습니다:

```java
public class MyFeatureAutoConfiguration implements BeanDefinitionRegistrar {
    @Override
    public Collection<BeanDefinition> registerAdditionalBeanDefinitions(
            Collection<BeanDefinition> existingDefs) {
        // 조건부 빈 등록 로직
        return additionalBeans;
    }
}
```

### BeanPostProcessor

빈 생성 과정에 개입하여 추가 처리를 할 수 있습니다.

```java
@Component
public class TimingBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(String beanName, Object bean) {
        if (bean.getClass().isAnnotationPresent(Timed.class)) {
            return createTimingProxy(bean);
        }
        return bean;
    }
}
```

Sprout의 IoC 컨테이너는 Spring과 유사하지만 더 단순하고 예측 가능한 구조로 설계되어 있습니다. 생성자 주입만을 지원하고, 명확한 빈 생명주기를 제공하여 디버깅과 이해가 쉬운 것이 특징입니다.