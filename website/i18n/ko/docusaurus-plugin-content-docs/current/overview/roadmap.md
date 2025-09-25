# 🗺️ Roadmap

Sprout의 개발은 **핵심 프레임워크 기능 완성**과 **시스템 내부 동작 실험**에 중점을 두고 진행됩니다.

## Version History

| Release | Status | Highlights | Notes |
| --- | --- | --- | --- |
| **v0.2** | ✅ **Released** | AOP Core Delivered | `@Before`/`@After`/`@Around`, AspectJ pointcuts |
| **v0.3** | ✅ **Released** | Middleware & Global Interceptors | Filters + Interceptors chain |
| **v0.4** | ✅ **Released** | Data Access & Security Core | `JdbcTemplate`, `@Transactional`, AuthN/AuthZ |
| **v0.5** | ✅ **Released** | **NIO & Hybrid Server**, Async WebSocket | Selector loop, OP_WRITE management, graceful close |

---

## Current Status: v1.0 🎯

**Goal**: 안정적인 API와 프레임워크 성숙도 확보 – 명확한 문서화와 개발자 경험 제공

### v1.0 Features (In Progress)

### Core Stability

- ✅ **API Stabilization** - 최종 공개 API 설계
- ✅ **Comprehensive Documentation** - 가이드와 레퍼런스 완비
- 🔄 **Performance Optimization** - 벤치마킹과 튜닝
- 🔄 **Error Handling Improvements** - 디버깅과 오류 메시지 개선

### Developer Experience

- ✅ **Clear Documentation** - 아키텍처 가이드 및 튜토리얼
- ✅ **Quick Start Experience** - 몇 분 만에 시작 가능
- 🔄 **IDE Support** - IDE 통합 개선

---

## Post v1.0 Roadmap

### Lightweight ORM (v1.1-v1.2)

**Status**: 🔄 Planned

Sprout는 v1.0까지는 ORM을 배제했지만, 이후에는 학습용/실험용 ORM을 가볍게 추가합니다:

- **Entity Mapping**: JPA 스타일 어노테이션
- **Query DSL**: 타입 세이프 쿼리 작성
- **Relationship Mapping**: 일대다, 다대다 관계
- **Migration Support**: 스키마 진화 지원
- **Connection Pooling**: 기본적인 커넥션 풀 관리

---

### Thread Scheduling Tool (v1.3+)

**Status**: 🔄 Planned

Sprout는 서버 성능과 동시성을 탐구하기 위해 **스레드 스케줄링 툴**을 실험적으로 도입합니다. 이는 Netty의 이벤트 루프 모델에서 아이디어를 차용하되, 플랫폼 스레드 기반 스레드풀에서 동작하도록 설계합니다.

- **플랫폼 스레드 모드**: 기존 가상 스레드가 아닌, 전통적인 플랫폼 스레드 기반
- **이벤트 루프 유사 구조**: Netty의 이벤트 루프처럼 Task를 큐에 담고 선택적으로 분배
- **스케줄링 정책**: 라운드 로빈, 워크 스틸링 등 다양한 정책 실험
- **추상화 API**: 애플리케이션에서 직접 스케줄링 전략을 선택 가능

```java
// Future scheduling tool example
SproutScheduler scheduler = SproutSchedulers.newEventLoopStyle(threads = 4);

scheduler.submit(() -> {
    System.out.println("Task executed by " + Thread.currentThread().getName());
});

```

이 기능은 학습 목적이 강하며, **운영체제 수준의 스케줄링 이해**와 **서버 이벤트 처리 메커니즘**을 직접 다뤄볼 수 있는 기회가 됩니다.

---

## Design Principles

Sprout의 로드맵은 다음 원칙을 따릅니다:

### 🎯 **Clarity Over Complexity**

설명이 어려운 기능은 포함하지 않는다. 단순하고 명확하게.

### 🔧 **Hackability**

프레임워크의 모든 부분은 확장/수정/교체 가능해야 한다.

### 📈 **Extensibility**

새로운 기능은 기존 기능과 자연스럽게 연결되어야 한다.

### 🚀 **Performance**

프레임워크 오버헤드는 최소화. 측정하고 필요한 부분만 최적화한다.

### 🛡️ **Stability**

메이저 버전에서만 호환성 깨뜨림. 모든 기능은 테스트로 뒷받침한다.

---

## Timeline

:::info Aspirational Roadmap

이 로드맵은 목표 지향적이며, 커뮤니티 피드백과 기술적 도전, 리소스 상황에 따라 변경될 수 있습니다.

:::

**2025 Q4**: v1.0 릴리스 – 안정된 API와 문서화

**2026 Q1-Q2**: Lightweight ORM 개발 (v1.1-v1.2)

**2026 이후**: Thread Scheduling Tool (v1.3+)