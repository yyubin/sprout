# 🗺️ Roadmap

Sprout's development focuses on **completing core framework functionality** and **experimenting with system internals**.

## Version History

| Release | Status | Highlights | Notes |
| --- | --- | --- | --- |
| **v0.2** | ✅ **Released** | AOP Core Delivered | `@Before`/`@After`/`@Around`, AspectJ pointcuts |
| **v0.3** | ✅ **Released** | Middleware & Global Interceptors | Filters + Interceptors chain |
| **v0.4** | ✅ **Released** | Data Access & Security Core | `JdbcTemplate`, `@Transactional`, AuthN/AuthZ |
| **v0.5** | ✅ **Released** | **NIO & Hybrid Server**, Async WebSocket | Selector loop, OP_WRITE management, graceful close |

---

## Current Status: v1.0 🎯

**Goal**: Achieve stable APIs and framework maturity – provide clear documentation and developer experience

### v1.0 Features (In Progress)

### Core Stability

- ✅ **API Stabilization** - Final public API design
- ✅ **Comprehensive Documentation** - Complete guides and reference
- 🔄 **Performance Optimization** - Benchmarking and tuning
- 🔄 **Error Handling Improvements** - Better debugging and error messages

### Developer Experience

- ✅ **Clear Documentation** - Architecture guides and tutorials
- ✅ **Quick Start Experience** - Up and running in minutes
- 🔄 **IDE Support** - Improved IDE integration

---

## Post v1.0 Roadmap

### Lightweight ORM (v1.1-v1.2)

**Status**: 🔄 Planned

While Sprout excluded ORM until v1.0, we plan to add a lightweight learning/experimental ORM afterward:

- **Entity Mapping**: JPA-style annotations
- **Query DSL**: Type-safe query construction
- **Relationship Mapping**: One-to-many, many-to-many relationships
- **Migration Support**: Schema evolution support
- **Connection Pooling**: Basic connection pool management

---

### Thread Scheduling Tool (v1.3+)

**Status**: 🔄 Planned

Sprout will experimentally introduce a **thread scheduling tool** to explore server performance and concurrency. This borrows ideas from Netty's event loop model but is designed to work with platform thread-based thread pools.

- **Platform Thread Mode**: Based on traditional platform threads, not virtual threads
- **Event Loop-like Structure**: Queue tasks and selectively distribute them like Netty's event loops
- **Scheduling Policies**: Experiment with various policies like round-robin, work stealing
- **Abstraction API**: Applications can directly choose scheduling strategies

```java
// Future scheduling tool example
SproutScheduler scheduler = SproutSchedulers.newEventLoopStyle(threads = 4);

scheduler.submit(() -> {
    System.out.println("Task executed by " + Thread.currentThread().getName());
});
```

This feature is primarily for learning purposes, providing an opportunity to directly work with **OS-level scheduling understanding** and **server event processing mechanisms**.

---

## Design Principles

Sprout's roadmap follows these principles:

### 🎯 **Clarity Over Complexity**

Don't include features that are hard to explain. Keep it simple and clear.

### 🔧 **Hackability**

Every part of the framework should be extensible/modifiable/replaceable.

### 📈 **Extensibility**

New features should connect naturally with existing functionality.

### 🚀 **Performance**

Minimize framework overhead. Measure and optimize only where needed.

### 🛡️ **Stability**

Break compatibility only in major versions. All features must be backed by tests.

---

## Timeline

:::info Aspirational Roadmap

This roadmap is goal-oriented and may change based on community feedback, technical challenges, and resource availability.

:::

**2025 Q4**: v1.0 release – Stable APIs and documentation

**2026 Q1-Q2**: Lightweight ORM development (v1.1-v1.2)

**2026+**: Thread Scheduling Tool (v1.3+)