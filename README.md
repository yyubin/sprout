# 🌱 Sprout

**Sprout**는 순수 Java 17로 **Spring Framework**와 **Tomcat**의 핵심 아이디어를 직접 구현하여
경량 Web Application Framework를 실습·학습 목적으로 재현한 프로젝트입니다.
“씨앗이 발아하듯(Spring → Sprout)” 한 단계씩 핵심 개념을 **from scratch**로 키워‑나간 과정과 결과를 담고 있습니다.

---

## 목차

1. [프로젝트 목표](#프로젝트-목표)
2. [주요 기능](#주요-기능)
3. [모듈 & 패키지 구조](#모듈--패키지-구조)
4. [도메인 모델](#도메인-모델)
5. [핵심 컴포넌트 구현 상세](#핵심-컴포넌트-구현-상세)
6. [빌드 & 실행](#빌드--실행)
7. [API 사용 예시](#api-사용-예시)
8. [프로젝트 강점](#프로젝트-강점)
9. [로드맵 & TODO](#로드맵--todo)

---

## 프로젝트 목표

| 목표                    | 설명                                                                           |
| --------------------- | ---------------------------------------------------------------------------- |
| **Spring 핵심 메커니즘 복습** | IoC Container, DI, AOP, `@Component`‑스캔, Argument Resolver 등 내부 동작을 직접 설계·구현 |
| **Tomcat 동시성 모델 이해**  | 논블로킹은 아니지만 `ExecutorService` 기반 Thread‑pool 로직으로 간단한 HTTP 서버 구축              |
| **프레임워크 제작 감각 체득**    | Annotation‑Processing, Reflection, Proxy API 활용 경험 강화                        |
| **학습 자료 제공**          | 실습 중심의 소규모 코드베이스로 ‘Framework 내부가 궁금한 개발자’에게 참고 자료 제공                         |

---

## 주요 기능

| 분류                     | 구현체 / 설명                                                                                                |
| ---------------------- | ------------------------------------------------------------------------------------------------------- |
| **IoC 컨테이너**           | `Container`<br>‣ 싱글톤 레지스트리<br>‣ 런타임 클래스 등록/조회 (`byType`, `byName`)                                      |
| **DI & 컴포넌트 스캐닝**      | `ComponentScanner`<br>‣ `@Component/@Service/@Controller/@Repository` 자동 탐색<br>‣ `@Requires`를 통한 생성자 주입 |
| **AOP (Proxy 기반)**     | `@BeforeAuthCheck` + `MethodProxyHandler`<br>‣ 권한 체크를 메서드 전·후단에 삽입                                      |
| **HTTP 서버**            | `HttpServer`<br>‣ `ServerSocket` + `ExecutorService(10 threads)`<br>‣ Request 수신 → `RequestHandler`로 위임 |
| **Argument Resolver**  | `RequestHandler#resolveParameters`<br>‣ 메서드 시그니처 분석 후 Query‑Param / Body‑JSON 매핑                        |
| **Exception Resolver** | `ExceptionProcessor`<br>‣ `@ExceptionHandler` 메커니즘                                                      |
| **도메인 서비스**            | `Board`, `Member`, `Post`, `Comment` 등 CRUD 샘플 API                                                      |
| **인메모리 저장소**           | `InMemoryBoardRepository` … (Repository 패턴)                                                             |
| **세션·권한 관리**           | `Session` 전역 객체 + `MemberAuthService`                                                                   |

---

## 모듈 & 패키지 구조

```
sprout
 ┣ config/            # 컨테이너, 스캐너, AOP, 예외처리
 ┣ controller/        # REST Controller 계층 (annotation 기반 매핑)
 ┣ domain/            # 핵심 엔티티 (Board, Member, Post, Comment …)
 ┣ repository/        # In-Memory Repository 패턴 구현
 ┣ service/           # 비즈니스 로직 및 AOP 대상
 ┣ http/              # HttpServer, Request/Response 파서, DTO
 ┣ util/              # 공통 유틸리티 (Session 등)
 ┗ server/            # 애플리케이션 부트스트랩
```

---

## 도메인 모델

| 엔티티         | 핵심 필드                                              | 설명             |
| ----------- | -------------------------------------------------- | -------------- |
| **Board**   | `boardId`, `boardName`, `description`, `gradeList` | 게시판 정보 및 접근 권한 |
| **Member**  | `memberId`, `name`, `grade`                        | 사용자 및 권한 관리    |
| **Post**    | `postId`, `boardId`, `content`, `createdDate`      | 게시글            |
| **Comment** | `commentId`, `postId`, `content`, `createdDate`    | 댓글             |

> 위 모델들은 순수 자바 객체(POJO)이며, JPA 없이 Repository 계층에서 In-Memory 저장으로 관리됩니다.
> 필요 시 RDBMS 연동을 고려해 `Repository 인터페이스` → `JpaBoardRepository` 형태로 대체 가능합니다.

---

## 핵심 컴포넌트 구현 상세

### 1. IoC 컨테이너

**클래스:** `config.Container`

* **싱글톤 레지스트리** – `getInstance()` 로 전역 단일 인스턴스 유지.
* **객체 저장소** – `Map<Class<?>, Object> objectMap` 에 컴포넌트 인스턴스 보관.
* **조회 방법**
  ‣ `get(Class<T>)` : 정확한 타입으로 캐스팅 반환.
  ‣ `getByType(Class<?>)` : `isAssignableFrom` 비교로 인터페이스/부모 타입 매칭.
  ‣ `getByName(String)` : FQN 문자열 기반 조회.
* **장점** – 스프링 `DefaultListableBeanFactory` 의 핵심 기능을 소형화. 런타임 동적 등록·조회가 가능해 테스트 용이.

### 2. DI (의존성 주입)

**클래스:** `config.ComponentScanner`

* `org.reflections.Reflections` 로 **`@Component` 계열 애노테이션** 스캔.
* **우선순위 정렬** – `@Priority` 값 오름차순 → 의존 관계가 깊은 Bean(낮은 번호)이 먼저 등록.
* **생성자 주입** –

  1. 클래스에 `@Requires(dependsOn = {...})` 선언.
  2. `ComponentScanner` 가 해당 타입을 `Container` 에서 조회.
  3. `Constructor.newInstance()` 로 주입 완료.
* **인터페이스 바인딩** – 생성한 인스턴스를 구현 인터페이스에도 함께 `register` ⇒ 스프링의 ‘인터페이스 위주 DI’ 학습 효과.

### 3. AOP (권한 체크 예시)

**클래스:** `sprout.aop.MethodProxyHandler`

* **적용 대상** – `@BeforeAuthCheck` 가 붙은 서비스 메서드.
* **동작**

  1. `ComponentScanner` 가 서비스 인스턴스 생성 후 `MethodProxyHandler.createProxy(...)` 호출.
  2. `Proxy.newProxyInstance` 로 **JDK 동적 프록시** 반환 (스프링 AOP 동일 메커니즘).
  3. `invoke()` 내부에서 세션·권한 검사 → 실패 시 `NotLoggedInException / UnauthorizedAccessException` throw.
* **장점** – 핵심 로직(BoardService) 과 횡단 관심사(권한) 분리.

### 4. Argument Resolver

**클래스:** `http.request.RequestHandler`

* 컨트롤러 메서드 파라미터를 **QueryString / JSON Body** 로부터 매핑.
* `resolveParameters()` 로직
  ‣ `method.getParameters()` 로 파라미터 이름·타입 조회 (※ `-parameters` 컴파일 플래그 필요).
  ‣ 기본 타입(Long, String) 은 쿼리 파라미터 → DTO / VO 는 `ObjectMapper.convertValue()` 로 변환.
* 실제로 스프링 MVC 의 `HandlerMethodArgumentResolver` 패턴을 재현.

### 5. Thread‑pool 기반 HTTP 서버

**클래스:** `server.HttpServer`

* `new ServerSocket(port)` 로 블로킹 소켓 열고, `ExecutorService fixedThreadPool(10)` 사용.
* 각 `Socket` 은 `handleClient()` 로 위임 → CPU 바운드 작업 분리.
* 장점: Tomcat 의 ‘Worker Thread’ 모델을 단순화하여 구현 원리를 체험.

### 6. Reflection 활용 포인트

| 목적       | 사용 API / 위치                                        | 비고                |
| -------- | -------------------------------------------------- | ----------------- |
| 애노테이션 스캔 | `Reflections.getTypesAnnotatedWith`                | 컴포넌트 탐색           |
| 생성자 주입   | `Class#getDeclaredConstructor`, `newInstance`      | DI                |
| 메서드 매핑   | `Method.isAnnotationPresent`, `Annotation#value()` | `RequestHandler`  |
| 런타임 프록시  | `Proxy.newProxyInstance`, `InvocationHandler`      | AOP               |
| 파라미터 이름  | `Method#getParameters`                             | Argument Resolver |

### 7. Proxy API

* **JDK 동적 프록시**: 인터페이스 기반. 런타임에 `Proxy` 클래스 생성 → 실제 객체 앞에 세움.
* **핵심 클래스**: `java.lang.reflect.Proxy`, `java.lang.reflect.InvocationHandler`.
* **AOP 흐름 요약**

  1. 실제 객체(BoardService) → 2) 프록시 객체 반환 → 3) Controller 가 호출 → 4) `invoke()` 선처리(권한) → 5) 실제 메서드 실행.
* “Sprout 에서는 프록시를 **선언적(애노테이션) 방식** 으로 적용했으며, 필요시 CGLIB(구상 클래스 프록시) 로 확장 가능.”

---

## 빌드 & 실행

```bash
# 1) 프로젝트 클론
git clone https://github.com/your-id/sprout.git
cd sprout

# 2) 빌드 (Gradle)
./gradlew clean build

# 3) 서버 기동
java -jar build/libs/sprout.jar    # 기본 8080 포트
```

> **포트 변경**은 `HttpServer#serverStart(int port)` 호출부(bootstrap)에서 조정할 수 있습니다.

---

## API 사용 예시

### 게시판 생성

```
POST /boards/add
Content-Type: application/json

{
  "boardName": "notice",
  "description": "공지사항",
  "grade": "ADMIN"
}
```

### 게시판 조회

```
GET /boards/view?boardName=notice
```

응답:

```json
{
  "message": "SUCCESS",
  "data": [
    { "게시글 번호": 1, "게시글 이름": "환영합니다", "작성일": "2025-05-26T14:30:00Z" }
  ]
}
```

> `Member`, `Post`, `Comment` 엔드포인트도 동일한 패턴으로 제공됩니다.

---

## 프로젝트 강점

| 강점                  | 상세 설명                                                     |
| ------------------- | --------------------------------------------------------- |
| **🔬 학습 친화적**       | Spring/Tomcat 내부 동작을 ‘보이는 코드’로 경험 → 프레임워크 블랙박스 탈피         |
| **🧩 모듈화 & 확장성**    | Annotation 기반 의존성 주입, 인터페이스 분리, 레이어드 아키텍처                 |
| **🛠 최소 의존**        | Lombok·Jackson·Reflections 외 추가 프레임워크 없음 → 순수 Java 핵심만 활용 |
| **🚦 AOP 기반 권한 제어** | `@BeforeAuthCheck` 로직으로 인증/인가를 서비스 코드와 분리                 |
| **⚙️ 경량 서버**        | Tomcat 대비 학습용으로 단순화된 HTTP 서버 → 디버깅 및 성능 실험에 용이            |
| **🧪 테스트 용이**       | 컨테이너가 POJO 객체를 반환하므로 단위 테스트 구성이 간결                        |

---

## 로드맵 & TODO

* [ ] **JDBC / JPA 연동** : 인메모리 → 영속 계층 전환
* [ ] **미들웨어 계층 추가** : 필터/인터셉터 체인, 로깅, CORS
* [ ] **비동기 I/O** : NIO 채널 + Selector 적용 (Reactor 패턴)
* [ ] **Swagger-like 문서화** : 어노테이션 메타데이터로 API 문서 자동 생성
* [ ] **Spring Boot 벤치마킹** : 동일 API 기준 성능 비교 리포트
* [ ] **통합 테스트 시나리오** : TestContainer 또는 Embedded DB 도입

---

## 라이선스

`MIT License` — 자유롭게 학습·수정·배포할 수 있지만, 출처 표기를 부탁드립니다 🌿

