# 🏃‍♂️ 빠른 시작

몇 분 만에 Sprout를 실행해보세요.

## 사전 요구사항

- **Java 21+** (가상 스레드 및 최신 언어 기능 필수)
- **Gradle** (프로젝트 빌드용)

## 1. 클론 및 빌드

```bash
$ git clone https://github.com/yyubin/sprout.git
$ cd sprout && ./gradlew build
```

## 2. 애플리케이션 실행

:::info Java 21 모듈 요구사항
CGLIB 프록시는 깊은 리플렉션을 위해 추가 모듈 접근 플래그가 필요합니다.
:::

```bash
$ java \
  --add-opens=java.base/java.lang=ALL-UNNAMED \
  --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
  --add-opens=java.base/java.io=ALL-UNNAMED \
  --add-opens=java.base/java.util=ALL-UNNAMED \
  -jar build/libs/sprout.jar
```

:::tip
서버 모드와 스레드 모델은 명령줄 인수가 아닌 `application.yml`을 통해 구성됩니다.
:::

## 3. 첫 번째 애플리케이션

### 메인 애플리케이션 클래스

```java
@ComponentScan("com.example.app")
public class DemoApplication {
    public static void main(String[] args) throws Exception {
        SproutApplication.run(DemoApplication.class);
    }
}
```

### 간단한 컨트롤러

```java
@Controller
@RequestMapping("/api")
public class HelloController {
    private final GreetingService service;

    public HelloController(GreetingService service) {
        this.service = service;
    }

    @GetMapping("/hello/{id}")
    public MessageDto hello(@PathVariable Long id) {
        return new MessageDto(service.greet(id));
    }

    @PostMapping("/users")
    public ResponseEntity<User> createUser(@RequestBody CreateUserRequest request) {
        User user = service.createUser(request);
        return ResponseEntity.ok(user);
    }
}
```

### AOP를 사용한 서비스

```java
@Service
public class GreetingService {
    public String greet(Long id) {
        return "안녕하세요, 사용자 " + id + "님!";
    }

    public User createUser(CreateUserRequest request) {
        // 비즈니스 로직 구현
        return new User(request.getName(), request.getEmail());
    }
}
```

### AOP Aspect

```java
@Aspect
public class LoggingAspect {
    @Around(pointcut = "com.example.app..*Service.*")
    public Object logExecutionTime(ProceedingJoinPoint pjp) throws Throwable {
        long startTime = System.nanoTime();
        try {
            return pjp.proceed();
        } finally {
            long duration = (System.nanoTime() - startTime) / 1_000;
            System.out.printf("%s 실행 시간: %d µs%n",
                pjp.getSignature().toLongName(), duration);
        }
    }
}
```

## 4. 구성 설정

`src/main/resources/application.yml` 파일을 생성하세요.

```yaml
author: 당신의-이름
server:
  execution-mode: hybrid   # nio | hybrid | blocking
  thread-type: virtual     # virtual | platform
  thread-pool-size: 150    # thread-type = platform일 때만 사용

sprout:
  database:
    url: jdbc:mysql://localhost:3306/myapp
    username: root
    password: your-password
```

## 5. 애플리케이션 테스트

실행 후 엔드포인트를 테스트해보세요.

```bash
# GET 요청
curl http://localhost:8080/api/hello/123

# POST 요청
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "홍길동", "email": "hong@example.com"}'
```

## 다음 단계는?

- 🏗️ [아키텍처 이해하기](../architecture/ioc-container) - Sprout의 작동 원리 이해

## 문제가 발생했나요?

- [일반적인 문제들](https://github.com/yyubin/sprout/issues) 찾아보기
- [커뮤니티 토론](https://github.com/yyubin/sprout/discussions)에 참여하기