# 🌱 **Sprout**  
경량화된 Java 웹 프레임워크를 직접 구현하며 Spring Framework의 동작 원리를 학습하는 프로젝트입니다.  
“바퀴 두 번 만들기”를 지향하되 **단순성·가독성·확장성**을 핵심 가치로 삼습니다.

---

## ✨ 주요 특징

| 기능 영역                 | 현재 구현 현황                                                                                                                                                                                                                                                            |
|-----------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **IoC / DI 컨테이너**     | `@Component @Service @Controller @Repository @Configuration` 스캔 및 싱글톤 관리<br/>– 생성자 기반 주입<br/>– `List<T>` 타입 자동 주입<br/>– 순환 의존성 탐지(위상 정렬)                                                                                                                            |
| **빈 정의**              | `ConstructorBeanDefinition` / `MethodBeanDefinition`<br/>– 생성자 vs. 팩터리 메서드 두 가지 전략 지원                                                                                                                                                                               |
| **Configuration 프록시** | `@Configuration(proxyBeanMethods = true)` 시 CGLIB 프록시 적용 → `@Bean` 메서드 호출 캐싱                                                                                                                                                                                        |
| **웹 계층**              | - `RequestMappingRegistry` + 패턴 매칭(`{var}` 지원)<br/>- `HandlerMethodScanner`로 컨트롤러 자동 등록<br/>- `PathVariable`, `RequestParam` 등 ArgumentResolver<br/>- `RequestDispatcher`로 요청 파싱·바인딩·호출<br />-`ResponseEntity`를 통한 응답 표준화 & `ResponseResolver`를 통한 DTO/void 자동 변환 지원 |
| **서버**                | **설정 기반 스레딩 모델** : application.yml 설정으로 가상 스레드(기본값)와 플랫폼 스레드 풀 간 전환 가능                                                                                                                                                                                              |
| **설정**                | **YAML 설정 지원** : `application.yml`을 통한 외부 설정 주입 (AppConfig)                                                                                                                                                                                                           |
| **예외 처리**             | 사용자 정의 예외(`BadRequestException`, `UnsupportedHttpMethod` 등) 기본 처리                                                                                                                                                                                                   |
| **부트 스트랩**            | `SproutApplication.run()` 한 줄로 컨테이너 초기화 & 서버 기동                                                                                                                                                                                                                     |

---

## 🏃‍♂️ 빠르게 시작하기
1. 프로젝트 클론 및 빌드
```bash
# 프로젝트 클론
git clone https://github.com/yyubin/sprout.git
cd sprout

# Gradle 빌드
./gradlew build
```

2. 애플리케이션 실행
   Java 17 이상에서는 CGLIB 프록시 생성을 위해 JVM 옵션이 필요합니다.  
   가상 스레드 사용을 위해서는, Java 21 이상이 필요합니다.

```bash
java --add-opens=java.base/java.lang=ALL-UNNAMED \
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
--add-opens=java.base/java.io=ALL-UNNAMED \
--add-opens=java.base/java.util=ALL-UNNAMED \
-jar build/libs/sprout.jar
```

3. 예제 코드
   SproutApplication은 지정된 패키지를 스캔하여 컨테이너를 구성하고 내장 서버를 실행합니다.

```java
// DemoApplication.java
@ComponentScan("app")     // 스캔 범위
public class DemoApplication {
    public static void main(String[] args) throws Exception {
        SproutApplication.run(DemoApplication.class);
    }
}
```
컨트롤러는 DTO 또는 ResponseEntity를 반환하여 유연하게 응답을 제어할 수 있습니다.

```java
// app/TestController.java
@Controller
@RequestMapping("/api")
public class TestController {

    private final TestService service;

    public TestController(TestService service) {
        this.service = service;
    }

    // DTO를 직접 반환하면 프레임워크가 ResponseEntity.ok()로 자동 변환합니다.
    @GetMapping("/hello/{id}")
    public MessageDto hello(@PathVariable Long id) {
        String message = service.greet(id);
        return new MessageDto(message);
    }
    
    // ResponseEntity를 반환하여 상태 코드와 헤더를 직접 제어할 수 있습니다.
    @PostMapping("/greetings")
    public ResponseEntity<MessageDto> createGreeting(@RequestBody GreetingRequest request) {
        MessageDto newGreeting = service.create(request);
        return ResponseEntity.created(newGreeting); // 201 Created 응답
    }
}
```

---

## 🗺️ 로드맵

| 단계       | 예정 기능            | 간략 설명                                                        |
| -------- | ---------------- |--------------------------------------------------------------|
| **v0.2** | **AOP**          | CGLIB 기반 프록시를 활용한 `@Around` 어드바이스 구현 (로깅, 트랜잭션 등)              |
| **v0.3** | **미들웨어 계층**      | Interceptor 파이프라인 설계, 전역 예외 처리, `@Cacheable` 어노테이션 기반 캐시                  |
| **v0.4** | **NIO 서버 전환** | `java.nio.channels` 기반의 논블로킹 I/O 서버 모델 도입 검토|
| **v0.5** | **데이터 접근 계층** | `JdbcTemplate` 패턴 도입 및 간단한 `RowMapper` 기반 ORM 기능 지원                                 |
| **v1.0** | **프로덕션 릴리스**     | 안정화 & 문서화 완료                                                 |

> ⚠️ **변경 가능성**: 학습 목적의 프로젝트이므로 구현 우선순위가 바뀔 수 있습니다.

---

## 🙏 Acknowledgements

* **Spring Framework** — 영감을 준 레퍼런스 아키텍처
* **Reflections**, **CGLIB**, **Jackson** — 런타임 메타프로그래밍 & 직렬화

