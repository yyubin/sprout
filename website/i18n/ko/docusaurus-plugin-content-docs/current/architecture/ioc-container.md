# IoC 컨테이너

Sprout의 핵심인 IoC(Inversion of Control) 컨테이너는 의존성 주입을 통해 애플리케이션 구성 요소를 관리합니다.

## 개요

IoC 컨테이너는 다음과 같은 기능을 제공합니다:

- **빈 스캐닝**: `@Component`, `@Service`, `@Controller`, `@Repository` 어노테이션이 있는 클래스 자동 감지
- **생성자 주입**: 의존성 자동 주입
- **생명주기 관리**: 빈의 생성, 초기화, 소멸 관리
- **순환 의존성 감지**: 위상 정렬을 통한 순환 의존성 감지

## 빈 등록

### 어노테이션 기반 등록

```java
@Component
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}

@Repository
public class UserRepository {
    // 구현...
}
```

### 설정 클래스를 통한 등록

```java
@Configuration
public class AppConfig {

    @Bean
    public DataSource dataSource() {
        return new HikariDataSource();
    }
}
```

## 의존성 주입

### 생성자 주입 (권장)

```java
@Service
public class OrderService {
    private final PaymentService paymentService;
    private final OrderRepository orderRepository;

    // 생성자 주입 - 자동으로 의존성이 주입됩니다
    public OrderService(PaymentService paymentService,
                       OrderRepository orderRepository) {
        this.paymentService = paymentService;
        this.orderRepository = orderRepository;
    }
}
```

### 리스트 주입

```java
@Service
public class NotificationService {
    private final List<NotificationProvider> providers;

    // 같은 타입의 모든 빈이 자동으로 리스트로 주입됩니다
    public NotificationService(List<NotificationProvider> providers) {
        this.providers = providers;
    }
}
```

## 스코프

### 싱글톤 스코프 (기본)

```java
@Component // 기본적으로 싱글톤
public class SingletonService {
    // 애플리케이션 전체에서 하나의 인스턴스만 생성됩니다
}
```

### 프로토타입 스코프

```java
@Component
@Scope("prototype")
public class PrototypeService {
    // 매번 새로운 인스턴스가 생성됩니다
}
```

## 컨테이너 라이프사이클

1. **클래스패스 스캐닝**: 지정된 패키지에서 어노테이션이 있는 클래스 발견
2. **빈 정의 생성**: 발견된 클래스에 대한 `BeanDefinition` 객체 생성
3. **의존성 분석**: 각 빈의 의존성 관계 분석
4. **순환 의존성 체크**: 위상 정렬로 순환 의존성 감지
5. **빈 인스턴스화**: 의존성 순서에 따라 빈 생성
6. **후처리**: `BeanPostProcessor`를 통한 빈 후처리

## 고급 기능

### 빈 후처리기

```java
@Component
public class CustomBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        // 초기화 전 처리
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // 초기화 후 처리
        return bean;
    }
}
```

### 조건부 빈 등록

```java
@Component
@ConditionalOnProperty(name = "feature.enabled", havingValue = "true")
public class ConditionalService {
    // feature.enabled=true일 때만 등록됩니다
}
```

## 내부 구현

Sprout의 IoC 컨테이너는 다음과 같은 핵심 컴포넌트로 구성됩니다:

- **`ApplicationContext`**: 컨테이너의 중앙 인터페이스
- **`BeanFactory`**: 빈 생성 및 관리 담당
- **`BeanDefinition`**: 빈의 메타데이터 저장
- **`ComponentScanner`**: 클래스패스 스캐닝 담당
- **`DependencyInjector`**: 의존성 주입 처리

이러한 설계를 통해 Spring과 유사하면서도 교육 목적에 최적화된 IoC 컨테이너를 제공합니다.