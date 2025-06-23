# 🌱 **Sprout**  
경량화된 Java 웹 프레임워크를 직접 구현하며 Spring Framework의 동작 원리를 학습하는 오픈소스 프로젝트입니다.  
“바퀴 두 번 만들기”를 지향하되 **단순성·가독성·확장성**을 핵심 가치로 삼습니다.

---

## ✨ 주요 특징

| 기능 영역 | 현재 구현 현황 |
|-----------|---------------|
| **IoC / DI 컨테이너** | `@Component @Service @Controller @Repository @Configuration` 스캔 및 싱글톤 관리<br/>– 생성자 기반 주입<br/>– `List<T>` 타입 자동 주입<br/>– 순환 의존성 탐지(위상 정렬) |
| **빈 정의** | `ConstructorBeanDefinition` / `MethodBeanDefinition`<br/>– 생성자 vs. 팩터리 메서드 두 가지 전략 지원 |
| **Configuration 프록시** | `@Configuration(proxyBeanMethods = true)` 시 CGLIB 프록시 적용 → `@Bean` 메서드 호출 캐싱 |
| **웹 계층** | - `RequestMappingRegistry` + 패턴 매칭(`{var}` 지원)<br/>- `HandlerMethodScanner`로 컨트롤러 자동 등록<br/>- `PathVariable`, `RequestParam` 등 ArgumentResolver<br/>- `RequestDispatcher`로 요청 파싱·바인딩·호출 |
| **서버** | 기존 `java.net.ServerSocket` + 고정 스레드풀(16) |
| **예외 처리** | 사용자 정의 예외(`BadRequestException`, `UnsupportedHttpMethod` 등) 기본 처리 |
| **부트 스트랩** | `SproutApplication.run()` 한 줄로 컨테이너 초기화 & 서버 기동 |

---

## 🏃‍♂️ 빠르게 시작하기

```bash
# 1. 프로젝트 클론
git clone https://github.com/yyubin/sprout.git
cd sprout

# 2. 빌드 & 실행
./gradlew build
java -jar build/libs/sprout.jar
````

```java
// DemoApplication.java
@ComponentScan("app")     // 스캔 범위
public class DemoApplication {
    public static void main(String[] args) throws Exception {
        SproutApplication.run(DemoApplication.class);
    }
}
```

```java
// app/TestController.java
@Controller
@RequestMapping("/api")
public class TestController {

    private final TestService service;

    public TestController(TestService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public String hello(@PathVariable Long id,
                        @RequestParam(value = "msg", required = false) String msg) {
        return service.greet(id, msg);
    }
}
```

---

## 🗺️ 로드맵

| 단계       | 예정 기능            | 간략 설명                                                         |
| -------- | ---------------- | ------------------------------------------------------------- |
| **v0.2** | **AOP**          | `@Around` 어드바이스 & 프록시 연결                 |
| **v0.3** | **미들웨어 계층**      | Interceptor / Filter 파이프라인 설계 & 전역 예외처리                       |
| **v0.4** | **가상 스레드 & NIO** | Java 21 Virtual Thread + `AsynchronousChannel` 기반 논블로킹 I/O 서버 |
| **v0.5** | **Plug-in 아키텍처** | 모듈화된 확장 포인트(예: 보안, 캐싱, 모니터링)                                  |
| **v1.0** | **프로덕션 릴리스**     | 안정화 & 문서화 완료                                                  |

> ⚠️ **변경 가능성**: 학습 목적의 프로젝트이므로 구현 우선순위가 바뀔 수 있습니다.

---

## 🙏 Acknowledgements

* **Spring Framework** — 영감을 준 레퍼런스 아키텍처
* **Reflections**, **CGLIB**, **Jackson** — 런타임 메타프로그래밍 & 직렬화

