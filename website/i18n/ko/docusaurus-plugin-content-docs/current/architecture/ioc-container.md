# 🏗️ IoC Container

IoC(Inversion of Control) 컨테이너는 Sprout Framework의 핵심입니다. 애플리케이션의 모든 컴포넌트에 대한 객체 생성, 의존성 주입, 생명주기를 관리합니다.

## 개요

Sprout의 IoC 컨테이너는 다음 기능을 제공합니다:
- **컴포넌트 스캔**: Reflections 라이브러리를 이용한 어노테이션 기반 클래스 자동 감지
- **생성자 주입**: 타입 안전한 의존성 해결 (필드 주입 미지원)
- **생명주기 관리**: 단계별(Phase) 빈 생성, 초기화, 소멸
- **순환 의존성 감지**: BeanGraph를 통한 위상 정렬과 순환 참조 감지
- **순서 지원**: @Order를 통한 빈 초기화 및 컬렉션 순서 제어
- **CGLIB 프록시**: @Configuration 클래스의 싱글톤 보장
- **전략 패턴 기반 확장성**: 빈 생성 전략과 의존성 해결 전략의 플러그인 구조

## 컨테이너 아키텍처

### 핵심 컴포넌트

Sprout의 IoC 컨테이너는 다음 주요 클래스들로 구성됩니다.

#### 컨텍스트 및 팩토리
- `SproutApplicationContext`: 메인 애플리케이션 컨텍스트
- `DefaultListableBeanFactory`: 핵심 빈 팩토리 구현
- `ClassPathScanner`: 클래스패스 스캔 및 빈 정의 생성
- `BeanGraph`: 의존성 그래프와 위상 정렬

#### 빈 생성 전략 (Strategy Pattern)
- `BeanInstantiationStrategy`: 빈 인스턴스화 전략 인터페이스
  - `ConstructorBasedInstantiationStrategy`: 생성자 기반 빈 생성
  - `FactoryMethodBasedInstantiationStrategy`: 팩토리 메서드 기반 빈 생성

#### 의존성 해결 전략 (Chain of Responsibility Pattern)
- `DependencyResolver`: 의존성 해결 인터페이스
  - `CompositeDependencyResolver`: 여러 resolver를 조합하는 복합 resolver
- `DependencyTypeResolver`: 타입별 의존성 해결 전략
  - `SingleBeanDependencyResolver`: 단일 빈 의존성 해결
  - `ListBeanDependencyResolver`: List 타입 의존성 해결

#### 생명주기 관리 (Phase Pattern)
- `BeanLifecycleManager`: 생명주기 단계 실행 관리자
- `BeanLifecyclePhase`: 생명주기 단계 인터페이스
  - `InfrastructureBeanPhase`: Infrastructure 빈 생성 (order=100)
  - `BeanPostProcessorRegistrationPhase`: BeanPostProcessor 등록 (order=200)
  - `ApplicationBeanPhase`: 애플리케이션 빈 생성 (order=300)
  - `ContextInitializerPhase`: ContextInitializer 실행 (order=400)

#### 타입 매칭 서비스
- `BeanTypeMatchingService`: 타입 기반 빈 검색 및 매칭 로직 중앙 관리

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

### 의존성 해결 아키텍처

Sprout v2.0부터 의존성 해결에 **Chain of Responsibility 패턴**을 적용하여 확장성을 크게 개선했습니다.

#### DependencyResolver 구조

```java
// 의존성 해결 인터페이스
public interface DependencyResolver {
    Object[] resolve(Class<?>[] dependencyTypes, Parameter[] params, BeanDefinition targetDef);
}

// 타입별 의존성 해결 전략
public interface DependencyTypeResolver {
    boolean supports(Class<?> type);
    Object resolve(Class<?> type, Parameter param, BeanDefinition targetDef);
}
```

#### 기본 제공 Resolver

1. **ListBeanDependencyResolver**: List 타입 의존성 처리
   - List 타입 파라미터를 감지하면 빈 리스트 생성
   - 제네릭 타입 정보를 추출하여 pending 목록에 등록
   - 나중에 `postProcessListInjections()`에서 실제 빈들을 주입

2. **SingleBeanDependencyResolver**: 단일 빈 의존성 처리
   - 일반적인 타입(List가 아닌)에 대해 BeanFactory에서 빈 조회
   - 타입 매칭 및 @Primary 선택 로직 활용

#### CompositeDependencyResolver

여러 `DependencyTypeResolver`를 체인으로 연결하여 순차적으로 시도합니다:

```java
public class CompositeDependencyResolver implements DependencyResolver {
    private final List<DependencyTypeResolver> typeResolvers;

    @Override
    public Object[] resolve(Class<?>[] dependencyTypes, Parameter[] params, BeanDefinition targetDef) {
        Object[] deps = new Object[dependencyTypes.length];

        for (int i = 0; i < dependencyTypes.length; i++) {
            Class<?> paramType = dependencyTypes[i];
            Parameter param = params[i];

            // 적절한 resolver를 찾아서 의존성 해결
            for (DependencyTypeResolver resolver : typeResolvers) {
                if (resolver.supports(paramType)) {
                    deps[i] = resolver.resolve(paramType, param, targetDef);
                    break;
                }
            }
        }
        return deps;
    }
}
```

#### 확장 방법

새로운 의존성 타입(예: Optional, Provider)을 지원하려면 `DependencyTypeResolver`를 구현하고 `DefaultListableBeanFactory` 생성자에 추가하면 됩니다:

```java
public class OptionalBeanDependencyResolver implements DependencyTypeResolver {
    @Override
    public boolean supports(Class<?> type) {
        return Optional.class.isAssignableFrom(type);
    }

    @Override
    public Object resolve(Class<?> type, Parameter param, BeanDefinition targetDef) {
        // Optional 처리 로직
        Class<?> genericType = extractGenericType(param);
        try {
            Object bean = beanFactory.getBean(genericType);
            return Optional.of(bean);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
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

### 빈 인스턴스화 전략 (Strategy Pattern)

Sprout v2.0부터 빈 생성 로직에 **Strategy Pattern**을 적용하여 다양한 생성 방식을 지원합니다.

#### BeanInstantiationStrategy 인터페이스

```java
public interface BeanInstantiationStrategy {
    Object instantiate(BeanDefinition def, DependencyResolver dependencyResolver, BeanFactory beanFactory) throws Exception;
    boolean supports(BeanCreationMethod method);
}
```

#### 구현체들

**1. ConstructorBasedInstantiationStrategy**

생성자를 통한 빈 생성을 담당합니다:

```java
public class ConstructorBasedInstantiationStrategy implements BeanInstantiationStrategy {
    @Override
    public Object instantiate(BeanDefinition def, DependencyResolver dependencyResolver, BeanFactory beanFactory) throws Exception {
        Constructor<?> constructor = def.getConstructor();

        // 의존성 해결
        Object[] deps = dependencyResolver.resolve(
            def.getConstructorArgumentTypes(),
            constructor.getParameters(),
            def
        );

        // Configuration 클래스의 경우 CGLIB 프록시 생성
        if (def.isConfigurationClassProxyNeeded()) {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(def.getType());
            enhancer.setCallback(new ConfigurationMethodInterceptor(beanFactory));
            return enhancer.create(def.getConstructorArgumentTypes(), deps);
        } else {
            constructor.setAccessible(true);
            return constructor.newInstance(deps);
        }
    }

    @Override
    public boolean supports(BeanCreationMethod method) {
        return method == BeanCreationMethod.CONSTRUCTOR;
    }
}
```

**2. FactoryMethodBasedInstantiationStrategy**

팩토리 메서드(@Bean)를 통한 빈 생성을 담당합니다:

```java
public class FactoryMethodBasedInstantiationStrategy implements BeanInstantiationStrategy {
    @Override
    public Object instantiate(BeanDefinition def, DependencyResolver dependencyResolver, BeanFactory beanFactory) throws Exception {
        Object factoryBean = beanFactory.getBean(def.getFactoryBeanName());
        Method factoryMethod = def.getFactoryMethod();

        Object[] deps = dependencyResolver.resolve(
            def.getFactoryMethodArgumentTypes(),
            factoryMethod.getParameters(),
            def
        );

        factoryMethod.setAccessible(true);
        return factoryMethod.invoke(factoryBean, deps);
    }

    @Override
    public boolean supports(BeanCreationMethod method) {
        return method == BeanCreationMethod.FACTORY_METHOD;
    }
}
```

#### DefaultListableBeanFactory의 전략 활용

```java
public class DefaultListableBeanFactory implements BeanFactory, BeanDefinitionRegistry {
    private final List<BeanInstantiationStrategy> instantiationStrategies;
    private final DependencyResolver dependencyResolver;

    public DefaultListableBeanFactory() {
        // 전략 초기화
        this.instantiationStrategies = new ArrayList<>();
        this.instantiationStrategies.add(new ConstructorBasedInstantiationStrategy());
        this.instantiationStrategies.add(new FactoryMethodBasedInstantiationStrategy());

        // 의존성 resolver 초기화
        List<DependencyTypeResolver> typeResolvers = new ArrayList<>();
        typeResolvers.add(new ListBeanDependencyResolver(pendingListInjections));
        typeResolvers.add(new SingleBeanDependencyResolver(this));
        this.dependencyResolver = new CompositeDependencyResolver(typeResolvers);
    }

    public Object createBean(BeanDefinition def) {
        // 적절한 전략 선택
        BeanInstantiationStrategy strategy = findStrategy(def);

        // 전략을 사용하여 빈 생성
        Object beanInstance = strategy.instantiate(def, dependencyResolver, this);

        // BeanPostProcessor 처리
        Object processedBean = applyBeanPostProcessors(beanInstance, def.getName());

        return processedBean;
    }

    private BeanInstantiationStrategy findStrategy(BeanDefinition def) {
        for (BeanInstantiationStrategy strategy : instantiationStrategies) {
            if (strategy.supports(def.getCreationMethod())) {
                return strategy;
            }
        }
        throw new IllegalArgumentException("No strategy found for: " + def.getCreationMethod());
    }
}
```

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

## 생명주기 관리

Sprout v2.0부터 **Phase Pattern**을 도입하여 빈 생명주기를 명확한 단계로 분리하고 관리합니다.

### 컨테이너 초기화 과정 (리팩토링 후)

```java
@Override
public void refresh() throws Exception {
    // 1. 빈 정의 스캔
    scanBeanDefinitions();

    // 2. BeanLifecycleManager를 통한 단계별 실행
    BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
            beanFactory,
            infraDefs,
            appDefs,
            basePackages
    );

    lifecycleManager.executePhases(context);
}
```

이전 버전의 복잡한 메서드 호출(`instantiateInfrastructureBeans()`, `instantiateAllSingletons()` 등)이 모두 Phase로 캡슐화되어 **19줄에서 10줄로 단순화**되었습니다.

### BeanLifecyclePhase 인터페이스

각 생명주기 단계를 나타내는 인터페이스입니다:

```java
public interface BeanLifecyclePhase {
    String getName();
    int getOrder();
    void execute(PhaseContext context) throws Exception;

    class PhaseContext {
        private final BeanFactory beanFactory;
        private final List<BeanDefinition> infraDefs;
        private final List<BeanDefinition> appDefs;
        private final List<String> basePackages;
        // getters...
    }
}
```

### 생명주기 단계 (Phases)

#### 1. InfrastructureBeanPhase (order=100)

Infrastructure 빈(BeanPostProcessor, InfrastructureBean)을 먼저 생성합니다:

```java
public class InfrastructureBeanPhase implements BeanLifecyclePhase {
    @Override
    public void execute(PhaseContext context) throws Exception {
        DefaultListableBeanFactory factory = (DefaultListableBeanFactory) context.getBeanFactory();

        // 위상 정렬 후 순서대로 생성
        List<BeanDefinition> order = new BeanGraph(context.getInfraDefs()).topologicallySorted();
        order.forEach(factory::createBean);

        // List 주입 후처리
        factory.postProcessListInjections();

        // PostInfrastructureInitializer 실행
        List<PostInfrastructureInitializer> initializers =
            factory.getAllBeans(PostInfrastructureInitializer.class);
        for (PostInfrastructureInitializer initializer : initializers) {
            initializer.afterInfrastructureSetup(factory, context.getBasePackages());
        }
    }

    @Override
    public int getOrder() { return 100; }
}
```

#### 2. BeanPostProcessorRegistrationPhase (order=200)

모든 BeanPostProcessor를 BeanFactory에 등록합니다:

```java
public class BeanPostProcessorRegistrationPhase implements BeanLifecyclePhase {
    @Override
    public void execute(PhaseContext context) {
        DefaultListableBeanFactory factory = (DefaultListableBeanFactory) context.getBeanFactory();

        List<BeanPostProcessor> allBeanPostProcessor =
            factory.getAllBeans(BeanPostProcessor.class);

        for (BeanPostProcessor beanPostProcessor : allBeanPostProcessor) {
            factory.addBeanPostProcessor(beanPostProcessor);
        }
    }

    @Override
    public int getOrder() { return 200; }
}
```

#### 3. ApplicationBeanPhase (order=300)

애플리케이션 빈을 생성합니다:

```java
public class ApplicationBeanPhase implements BeanLifecyclePhase {
    @Override
    public void execute(PhaseContext context) {
        DefaultListableBeanFactory factory = (DefaultListableBeanFactory) context.getBeanFactory();

        // 위상 정렬 후 순서대로 생성
        List<BeanDefinition> order = new BeanGraph(context.getAppDefs()).topologicallySorted();
        order.forEach(factory::createBean);

        // List 주입 후처리
        factory.postProcessListInjections();
    }

    @Override
    public int getOrder() { return 300; }
}
```

#### 4. ContextInitializerPhase (order=400)

모든 ContextInitializer를 실행합니다:

```java
public class ContextInitializerPhase implements BeanLifecyclePhase {
    @Override
    public void execute(PhaseContext context) {
        BeanFactory beanFactory = context.getBeanFactory();

        List<ContextInitializer> contextInitializers =
            beanFactory.getAllBeans(ContextInitializer.class);
        for (ContextInitializer initializer : contextInitializers) {
            initializer.initializeAfterRefresh(beanFactory);
        }
    }

    @Override
    public int getOrder() { return 400; }
}
```

### BeanLifecycleManager

모든 Phase를 순서대로 실행하는 매니저입니다:

```java
public class BeanLifecycleManager {
    private final List<BeanLifecyclePhase> phases;

    public BeanLifecycleManager(List<BeanLifecyclePhase> phases) {
        this.phases = phases.stream()
                .sorted(Comparator.comparingInt(BeanLifecyclePhase::getOrder))
                .toList();
    }

    public void executePhases(BeanLifecyclePhase.PhaseContext context) throws Exception {
        for (BeanLifecyclePhase phase : phases) {
            System.out.println("--- Executing Phase: " + phase.getName() +
                " (order=" + phase.getOrder() + ") ---");
            phase.execute(context);
        }
    }
}
```

### 생명주기 확장

새로운 단계를 추가하려면 `BeanLifecyclePhase`를 구현하고 `SproutApplicationContext` 생성자에 등록하면 됩니다:

```java
public class CustomPhase implements BeanLifecyclePhase {
    @Override
    public String getName() {
        return "Custom Initialization Phase";
    }

    @Override
    public int getOrder() {
        return 250;  // BeanPostProcessor 등록 후, 애플리케이션 빈 생성 전
    }

    @Override
    public void execute(PhaseContext context) throws Exception {
        // 커스텀 초기화 로직
    }
}

// SproutApplicationContext 생성자에서
List<BeanLifecyclePhase> phases = new ArrayList<>();
phases.add(new InfrastructureBeanPhase());
phases.add(new BeanPostProcessorRegistrationPhase());
phases.add(new CustomPhase());  // 커스텀 Phase 추가
phases.add(new ApplicationBeanPhase());
phases.add(new ContextInitializerPhase());
this.lifecycleManager = new BeanLifecycleManager(phases);
```

### 빈 생성 순서

각 Phase 내에서 `BeanGraph`를 이용해 의존성 그래프를 분석하고 위상 정렬하여 올바른 순서로 빈을 생성합니다:

```java
// 위상 정렬로 의존성 순서 결정
List<BeanDefinition> order = new BeanGraph(defs).topologicallySorted();

// 순서대로 빈 생성
order.forEach(beanFactory::createBean);

// 컬렉션 주입 후처리
beanFactory.postProcessListInjections();
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

## 아키텍처 리팩토링 요약 (v2.0)

### 변경 동기

Sprout v1.x의 IoC 컨테이너는 다음과 같은 한계가 있었습니다:

- `DefaultListableBeanFactory`가 너무 많은 책임을 가짐 (SRP 위반)
- 빈 생성 로직이 단일 메서드에 집중되어 확장이 어려움
- 의존성 해결 로직이 경직되어 새로운 타입 추가가 힘듦
- 생명주기 관리가 하드코딩되어 새로운 단계 추가가 복잡함
- 타입 매칭 로직이 중복됨 (BeanGraph vs BeanFactory)

### 적용된 디자인 패턴

#### 1. Strategy Pattern (빈 생성 전략)

**Before:**
```java
// createBean 메서드 내에 모든 생성 로직 집중 (50+ 줄)
if (def.getCreationMethod() == BeanCreationMethod.CONSTRUCTOR) {
    // 생성자 로직
} else if (def.getCreationMethod() == BeanCreationMethod.FACTORY_METHOD) {
    // 팩토리 메서드 로직
}
```

**After:**
```java
// 전략 패턴으로 분리
BeanInstantiationStrategy strategy = findStrategy(def);
Object beanInstance = strategy.instantiate(def, dependencyResolver, this);
```

**이점:**
- 새로운 생성 방식(빌더 패턴, 정적 팩토리 등) 추가 용이
- 각 전략을 독립적으로 테스트 가능
- OCP(개방-폐쇄 원칙) 준수

#### 2. Chain of Responsibility Pattern (의존성 해결)

**Before:**
```java
// resolveDependencies 메서드에서 if-else 분기
if (List.class.isAssignableFrom(paramType)) {
    // List 처리
} else {
    // 단일 빈 처리
}
```

**After:**
```java
// Resolver 체인으로 순차 처리
for (DependencyTypeResolver resolver : typeResolvers) {
    if (resolver.supports(paramType)) {
        return resolver.resolve(paramType, param, targetDef);
    }
}
```

**이점:**
- Optional, Provider 등 새로운 타입 지원 쉬움
- 각 resolver를 독립적으로 구현 및 테스트
- 확장성 극대화

#### 3. Phase Pattern (생명주기 관리)

**Before:**
```java
// refresh() 메서드에 하드코딩된 순서 (19줄)
scanBeanDefinitions();
instantiateInfrastructureBeans();
instantiateAllSingletons();
// ContextInitializer 실행...
```

**After:**
```java
// Phase 패턴으로 단순화 (10줄)
scanBeanDefinitions();
BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(...);
lifecycleManager.executePhases(context);
```

**이점:**
- 새로운 lifecycle 단계 추가가 간단함
- 각 단계의 책임이 명확히 분리됨
- 테스트 및 디버깅 용이

#### 4. Service 분리 (타입 매칭)

**Before:**
- `BeanGraph.getBeanNamesForType()`
- `DefaultListableBeanFactory.candidateNamesForType()`
- 중복된 로직

**After:**
```java
// BeanTypeMatchingService로 통합
public class BeanTypeMatchingService {
    public Set<String> findCandidateNamesForType(Class<?> type) { ... }
    public String choosePrimary(Class<?> requiredType, ...) { ... }
    public Set<String> getBeanNamesForType(Class<?> type) { ... }
}
```

**이점:**
- 타입 매칭 로직을 한 곳에서 관리
- BeanGraph와 BeanFactory 간 중복 제거
- 캐싱 전략 적용 가능

### 개선 결과

#### 정량적 개선
- **SproutApplicationContext.refresh()**: 19줄 → 10줄 (47% 감소)
- **DefaultListableBeanFactory**: 357줄 → 280줄 (22% 감소)
- **책임 분리**: 1개 클래스 → 15개 클래스 (단일 책임 원칙)

#### 정성적 개선
- ✅ 각 컴포넌트의 책임이 명확해짐
- ✅ 새로운 기능 추가가 쉬워짐 (OCP 준수)
- ✅ 테스트 가능성 향상
- ✅ 코드 가독성 및 유지보수성 향상
- ✅ Spring과 유사한 확장성 확보

### 하위 호환성

**모든 기존 동작 100% 보존:**
- Infrastructure 빈 우선 등록
- BeanPostProcessor 적시 등록
- PostInfrastructureInitializer에 패키지 정보 전달
- List 주입 후처리
- 위상 정렬 기반 의존성 순서 보장
- 순환 의존성 감지

### 향후 확장 방향

리팩토링된 아키텍처를 기반으로 다음 기능을 쉽게 추가할 수 있습니다:

1. **새로운 의존성 타입 지원**
   - `Optional<T>`: 선택적 의존성
   - `Provider<T>`: 지연 로딩
   - `Map<String, T>`: 이름별 빈 매핑

2. **새로운 빈 생성 방식**
   - 빌더 패턴 기반 생성
   - 정적 팩토리 메서드
   - 프로토타입 스코프

3. **새로운 생명주기 단계**
   - 이벤트 기반 확장성
   - Lazy 초기화 지원
   - 빈 생성 성능 모니터링

## 결론

Sprout의 IoC 컨테이너는 Spring과 유사하지만 더 단순하고 예측 가능한 구조로 설계되어 있습니다. v2.0 리팩토링을 통해 **전략 패턴, 책임 체인 패턴, Phase 패턴**을 적용하여 확장성과 유지보수성을 대폭 개선했습니다. 생성자 주입만을 지원하고, 명확한 빈 생명주기를 제공하여 디버깅과 이해가 쉬운 것이 특징입니다.