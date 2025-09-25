# 🌱 Sprout Framework

*Spring이 내부적으로 어떻게 작동하는지 이해하기 위해 처음부터 구축한 경량 Java 웹 프레임워크입니다.*

Sprout은 현대 Java 웹 개발의 핵심 개념을 바닥부터 구현한 포괄적인 웹 프레임워크입니다. **완전히 작동하는 NIO & 하이브리드 서버**와 **비동기 WebSocket 스택**을 통해 **명확성, 해킹 가능성, 확장성**에 중점을 두고 있습니다.

## 왜 Sprout인가?

**교육적 목적**: Spring과 같은 프레임워크가 내부적으로 어떻게 작동하는지 비슷한 기능을 처음부터 구축하여 이해합니다.

**프로덕션 준비**: 교육적 목적에도 불구하고, Sprout은 NIO 서버, 포괄적인 테스트(575개 테스트, 85% 라인 커버리지), 보안 시스템과 같은 프로덕션 수준의 기능을 포함합니다.

**집중된 범위**: 컨테이너/AOP/웹/서버 내부에 의도적으로 집중. v1.0에서는 완전한 ORM이 범위에서 제외되어 표면적을 관리 가능하게 유지하고 코드를 쉽게 감사할 수 있습니다.

## 주요 기능

<div className="feature-card">

### 🏗️ IoC / DI 컨테이너
- `@Component`, `@Service`, `@Controller`, `@Repository`, `@Configuration`, `@Aspect` 스캔
- 자동 `List<T>` 채우기와 함께 생성자 기반 주입
- 위상 정렬을 사용한 순환 의존성 감지
- 합리적인 기본값을 위한 `BeanDefinitionRegistrar`를 통한 자동 구성

</div>

<div className="feature-card">

### 🎯 AOP (관점 지향 프로그래밍)
- 어노테이션 주도 (`@Before`, `@After`, `@Around`)
- 와일드카드(`*`, `..`, `?`)를 포함한 AspectJ 스타일 포인트컷
- CGLIB 서브클래싱 + Objenesis 폴백 (인자가 없는 생성자 불필요)
- 타겟별 프록시가 있는 정렬된 어드바이저 체인

</div>

<div className="feature-card">

### 🌐 웹 레이어
- 선언적 라우팅 (`@GetMapping`, `@PostMapping` 등)
- 경로 변수(`{id}`)와 요청 매개변수 바인딩
- 포괄적인 인수 리졸버 및 응답 처리
- `@ControllerAdvice`를 통한 예외 처리

</div>

<div className="feature-card">

### ⚡ 고성능 서버
- `java.nio.channels`를 기반으로 구축된 **NIO 서버**
- **하이브리드 모드**: 가상 스레드를 통한 HTTP, NIO를 통한 WebSocket
- 학습 및 디버깅을 위한 블로킹 폴백
- 구성 가능한 실행 모드 및 스레드 유형

</div>

<div className="feature-card">

### 🔒 보안 시스템
- 모듈식 인증 (`AuthenticationManager`, `UserDetailsService`)
- `@PreAuthorize`를 통한 메서드 보안 (AOP 기반)
- URL 인가 및 역할 기반 접근 제어
- 요청별 `ThreadLocal`을 가진 `SecurityContextHolder`

</div>

<div className="feature-card">

### 🔌 WebSocket 지원
- RFC6455 호환 핸드셰이크 및 프레임 처리
- 우아한 연결 처리를 가진 논블로킹 쓰기 큐
- 큰 메시지를 위한 분할 지원
- 생명주기 훅 (`@OnOpen`, `@OnMessage`, `@OnClose`, `@OnError`)

</div>

## 품질 & 테스트

<div className="coverage-badge">575개 테스트</div>
<div className="coverage-badge">85% 라인 커버리지</div>
<div className="coverage-badge">75% 브랜치 커버리지</div>
<div className="coverage-badge">0개 실패</div>

포괄적인 테스트 슈트는 다음을 다룹니다:
- 핵심 컨테이너 기능 (스캔, 주입, 생명주기)
- AOP 어드바이스 빌더와 인터셉터
- MVC 요청/응답 처리
- 보안 인증 및 인가
- 서버 구현 (블로킹/NIO/하이브리드)
- WebSocket 프로토콜 준수

## 시작하기

Sprout으로 구축할 준비가 되셨나요? [빠른 시작 가이드](./overview/quickstart)를 확인하여 몇 분 안에 시작해보세요.

아키텍처를 이해하고 싶으신가요? [아키텍처 가이드](./architecture/ioc-container)를 탐색하여 Sprout이 IoC, AOP, 웹 서빙을 어떻게 구현하는지 알아보세요.

## 커뮤니티 & 지원

- 📚 [문서](./overview/quickstart) - 포괄적인 가이드 및 API 참조
- 🐛 [이슈](https://github.com/yyubin/sprout/issues) - 버그 리포트 및 기능 요청
- 💬 [토론](https://github.com/yyubin/sprout/discussions) - 커뮤니티 지원 및 아이디어
- 📊 [테스트 리포트](/tests/) - 최신 테스트 실행 결과
- 📈 [커버리지 리포트](/coverage/) - 코드 커버리지 분석