# 🗺️ Roadmap

Sprout's development follows a clear roadmap focused on delivering core framework functionality with production-ready quality.

## Version History

| Release | Status | Highlights | Notes |
|---------|--------|------------|-------|
| **v0.2** | ✅ **Released** | AOP Core Delivered | `@Before`/`@After`/`@Around`, AspectJ pointcuts |
| **v0.3** | ✅ **Released** | Middleware & Global Interceptors | Filters + Interceptors chain |
| **v0.4** | ✅ **Released** | Data Access & Security Core | `JdbcTemplate`, `@Transactional`, AuthN/AuthZ |
| **v0.5** | ✅ **Released** | **NIO & Hybrid Server**, Async WebSocket | Selector loop, OP_WRITE management, graceful close |

## Current Status: v1.0 🎯

**Goal**: Stable API & Framework Maturity - Ready for production use with comprehensive documentation

### v1.0 Features (In Progress)

#### Core Stability
- ✅ **API Stabilization** - Final public API design
- ✅ **Comprehensive Documentation** - Complete guides and reference
- 🔄 **Performance Optimization** - Benchmarking and tuning
- 🔄 **Error Handling Improvements** - Better error messages and debugging

#### Production Readiness
- ✅ **Robust Testing** - 575 tests with 85% line coverage
- 🔄 **Integration Testing** - End-to-end scenarios
- 🔄 **Production Deployment Guide** - Docker, cloud deployment
- 🔄 **Monitoring & Observability** - Metrics and health checks

#### Developer Experience
- ✅ **Clear Documentation** - Architecture guides and tutorials
- ✅ **Quick Start Experience** - Get running in minutes
- 🔄 **IDE Support** - Better tooling integration
- 🔄 **Migration Guides** - From other frameworks

## Post v1.0 Roadmap

### Lightweight ORM (v1.1-v1.2)
**Status**: 🔄 Planned

Sprout intentionally excludes a full ORM for v1.0 to keep the surface area manageable. Post-v1.0, we plan to add:

- **Entity Mapping**: JPA-style annotations
- **Query DSL**: Type-safe query building
- **Relationship Mapping**: One-to-many, many-to-many relations
- **Migration Support**: Schema evolution
- **Connection Pooling**: Advanced pool management

```java
// Future ORM example
@Entity
public class User {
    @Id @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String email;

    @OneToMany(mappedBy = "user")
    private List<Order> orders;
}

@Repository
public class UserRepository extends SproutRepository<User, Long> {
    List<User> findByEmailDomain(String domain) {
        return query()
            .where(User_.email.like("%" + domain))
            .orderBy(User_.createdAt.desc())
            .list();
    }
}
```

### Production Tools (v1.3)
**Status**: 🔄 Planned

- **Metrics Integration**: Micrometer/Prometheus support
- **Distributed Tracing**: OpenTelemetry integration
- **Health Checks**: Application health endpoints
- **Performance Profiling**: Built-in profiling tools

```java
// Future metrics example
@RestController
@Timed("user.controller")
public class UserController {
    @GetMapping("/users/{id}")
    @Counted("user.lookups")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
    }
}
```

### Advanced Features (v1.4+)
**Status**: 🔄 Planned

#### Enhanced Security
- **OAuth2/OIDC**: Modern authentication protocols
- **RBAC**: Role-based access control
- **Rate Limiting**: Request throttling
- **Security Headers**: CORS, CSP, HSTS

#### Caching Layer
- **Cache Annotations**: `@Cacheable`, `@CacheEvict`
- **Multiple Backends**: Redis, Hazelcast, local
- **Cache Statistics**: Hit ratios and performance

#### Validation Framework
- **Bean Validation**: JSR-303/349 compatible
- **Custom Validators**: Domain-specific validation
- **Error Handling**: Structured validation responses

## Design Principles

Our roadmap follows these core principles:

### 🎯 **Clarity Over Complexity**
Every feature must be understandable and well-documented. If it's too complex to explain clearly, it doesn't belong in Sprout.

### 🔧 **Hackability**
Users should be able to extend, modify, or replace any part of the framework. No black boxes.

### 📈 **Extensibility**
New features should integrate naturally with existing ones. Plugin architecture over monolithic design.

### 🚀 **Performance**
Framework overhead should be minimal. Measure everything, optimize what matters.

### 🛡️ **Stability**
Breaking changes only in major versions. Comprehensive testing for every feature.

## Community Input

We welcome community input on our roadmap:

- **Feature Requests**: [GitHub Issues](https://github.com/yyubin/sprout/issues)
- **Design Discussions**: [GitHub Discussions](https://github.com/yyubin/sprout/discussions)
- **Contributions**: [Contributing Guide](https://github.com/yyubin/sprout/blob/main/CONTRIBUTING.md)

## Timeline

:::info Aspirational Roadmap
This roadmap is aspirational and may change based on community feedback, technical challenges, and resource availability.
:::

**2024 Q4**: v1.0 release with stable APIs and comprehensive documentation

**2025 Q1-Q2**: Lightweight ORM development (v1.1-v1.2)

**2025 Q3**: Production tools and monitoring (v1.3)

**2025 Q4+**: Advanced features and ecosystem growth (v1.4+)

## Contributing

Want to help shape Sprout's future? Here's how:

1. **Try the Framework** - Build something with Sprout and share your experience
2. **Report Issues** - Help us identify bugs and usability problems
3. **Suggest Features** - What's missing for your use case?
4. **Write Code** - Pick up issues and submit pull requests
5. **Improve Docs** - Help make our documentation better

Together, let's build a framework that makes Java web development both powerful and enjoyable! 🌱