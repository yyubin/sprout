# ⚙️ 설정

Sprout는 시작 시 로드되는 YAML 기반 구성 시스템을 사용합니다. `AppConfig` 클래스가 파싱을 처리하고 구성 속성에 편리한 접근을 제공합니다.

## 설정 파일

프로젝트에 `src/main/resources/application.yml` 파일을 생성하세요:

```yaml
author: 당신의-이름

server:
  execution-mode: hybrid   # nio | hybrid | blocking (기본값: hybrid)
  thread-type: virtual     # virtual | platform (기본값: virtual)
  thread-pool-size: 150    # thread-type = platform일 때 사용

sprout:
  database:
    url: jdbc:mysql://localhost:3306/sprout
    username: root
    password: change-me

  security:
    enabled: true
    jwt:
      secret: your-jwt-secret-key
      expiration: 86400  # 24시간 (초 단위)
```

## 서버 구성

### 실행 모드

Sprout는 다양한 사용 사례에 적합한 세 가지 실행 모드를 지원합니다:

| 모드        | HTTP 핸들러             | WebSocket | 사용 사례                      |
|------------|------------------------|-----------|-------------------------------|
| `nio`      | NIO 셀렉터              | NIO       | 높은 동시성, 적은 메모리        |
| `hybrid`   | 가상/플랫폼 스레드 풀     | NIO       | 운영 환경 (권장)               |
| `blocking` | 플랫폼 스레드            | 지원 안함  | 디버깅/학습                    |

#### NIO 모드
```yaml
server:
  execution-mode: nio
```
- 모든 I/O에 대한 단일 스레드 셀렉터 루프
- 높은 연결 수에 대해 가장 메모리 효율적
- I/O 바운드 애플리케이션에 최적

#### Hybrid 모드 (권장)
```yaml
server:
  execution-mode: hybrid
  thread-type: virtual
```
- HTTP 요청은 가상 스레드 또는 플랫폼 풀로 처리
- WebSocket 연결은 NIO 셀렉터로 처리
- 성능과 리소스 사용량의 최적 균형

#### Blocking 모드
```yaml
server:
  execution-mode: blocking
  thread-type: platform
  thread-pool-size: 200
```
- 전통적인 요청당 하나의 스레드 모델
- 디버깅과 이해가 가장 쉬움
- 이 모드에서는 WebSocket이 지원되지 않음

### 스레드 유형

가상 스레드(Project Loom)와 기존 플랫폼 스레드 중 선택:

| 유형            | 사용 시기                                      | 구성                   |
|----------------|------------------------------------------------|----------------------|
| `virtual`      | 기본 선택 (권장)                                | `thread-type: virtual` |
| `platform`     | 제한된 동시성이 필요할 때                        | `thread-type: platform` + `thread-pool-size` |

```yaml
server:
  thread-type: virtual  # 무제한 가상 스레드
```

```yaml
server:
  thread-type: platform
  thread-pool-size: 150  # 고정 크기 스레드 풀
```

## 데이터베이스 구성

데이터베이스 연결을 구성하세요:

```yaml
sprout:
  database:
    url: jdbc:mysql://localhost:3306/myapp
    username: myuser
    password: mypassword
```

### CORS 구성
```yaml
cors:
  allow-origin: "*"
  allow-credentials: true
  allow-methods: "GET, POST, PUT, DELETE, OPTIONS"
  allow-headers: "Content-Type, Authorization"
  expose-headers: ""
  max-age: 3600
```

## 코드에서 구성 접근

`AppConfig`를 사용하여 구성 값에 접근하세요.

```java
@Component
public class MyService {
    private final AppConfig config;

    public MyService(AppConfig config) {
        this.config = config;
    }

    public void doSomething() {
        String author = config.getStringProperty("author");
        int poolSize = config.getIntProperty("server.thread-pool-size", 100);
        boolean securityEnabled = config.getBooleanProperty("sprout.security.enabled", false);
    }
}
```

## 구성 검증

Sprout는 시작 시 구성을 검증하고 유용한 오류 메시지를 제공합니다:

```yaml
# 잘못된 구성은 시작 실패를 야기합니다
server:
  execution-mode: invalid-mode  # 오류: 알 수 없는 실행 모드
  thread-pool-size: -1          # 오류: 풀 크기는 양수여야 함
```