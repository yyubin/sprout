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
5. [빌드 & 실행](#빌드--실행)
6. [API 사용 예시](#api-사용-예시)
7. [프로젝트 강점](#프로젝트-강점)
8. [로드맵 & TODO](#로드맵--todo)

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
