# 🌱 Sprout Framework

*A lightweight Java web framework built from scratch to demystify how Spring works under the hood.*

Sprout is a comprehensive web framework that implements the core concepts of modern Java web development from the ground up. With **fully working NIO & hybrid servers** and an **async WebSocket stack**, Sprout remains focused on **clarity, hackability, and extensibility**.

## Why Sprout?

**Educational Purpose**: Understanding how frameworks like Spring work internally by building similar functionality from scratch.

**Focused Scope**: Intentionally focused on container/AOP/web/server internals. A full ORM is out of scope for v1.0 to keep the surface area manageable and the code easy to audit.

## Key Features

<div className="feature-card">

### 🏗️ IoC / DI Container
- Scans `@Component`, `@Service`, `@Controller`, `@Repository`, `@Configuration`, `@Aspect`
- Constructor-based injection with automatic `List<T>` population
- Cyclic-dependency detection using topological sort
- Auto-configuration via `BeanDefinitionRegistrar` for sensible defaults

</div>

<div className="feature-card">

### 🎯 AOP (Aspect-Oriented Programming)
- Annotation-driven (`@Before`, `@After`, `@Around`)
- AspectJ-style pointcuts with wildcards (`*`, `..`, `?`)
- CGLIB subclassing + Objenesis fallback (no no-arg constructor required)
- Ordered advisor chain with proxy-per-target

</div>

<div className="feature-card">

### 🌐 Web Layer
- Declarative routing (`@GetMapping`, `@PostMapping`, etc.)
- Path variables (`{id}`) and request parameter binding
- Comprehensive argument resolvers and response handling
- Exception handling with `@ControllerAdvice`

</div>

<div className="feature-card">

### ⚡ High-Performance Server
- **NIO server** built on `java.nio.channels`
- **Hybrid mode**: HTTP over virtual threads, WebSocket over NIO
- Blocking fallback for learning and debugging
- Configurable execution modes and thread types

</div>

<div className="feature-card">

### 🔒 Security System
- Modular authentication (`AuthenticationManager`, `UserDetailsService`)
- Method security with `@PreAuthorize` (AOP-based)
- URL authorization and role-based access control
- `SecurityContextHolder` with per-request `ThreadLocal`

</div>

<div className="feature-card">

### 🔌 WebSocket Support
- RFC6455 compliant handshake and frame processing
- Non-blocking write queues with graceful connection handling
- Fragmentation support for large messages
- Lifecycle hooks (`@OnOpen`, `@OnMessage`, `@OnClose`, `@OnError`)

</div>

## Quality & Testing

<div className="coverage-badge">687 Tests</div>
<div className="coverage-badge">85% Line Coverage</div>
<div className="coverage-badge">75% Branch Coverage</div>
<div className="coverage-badge">0 Failures</div>

Our comprehensive test suite covers:
- Core container functionality (scanning, injection, lifecycle)
- AOP advice builders and interceptors
- MVC request/response handling
- Security authentication and authorization
- Server implementations (Blocking/NIO/Hybrid)
- WebSocket protocol compliance

## Getting Started

Ready to build with Sprout? Check out our [Quick Start Guide](./quickstart) to get up and running in minutes.

Want to understand the architecture? Explore our [Architecture Guide](../architecture/ioc-container) to learn how Sprout implements IoC, AOP, and web serving.

## Community & Support

- 📚 [Documentation](./quickstart) - Comprehensive guides and API reference
- 🐛 [Issues](https://github.com/yyubin/sprout/issues) - Bug reports and feature requests
- 💬 [Discussions](https://github.com/yyubin/sprout/discussions) - Community support and ideas
- 📊 [Test Reports](/tests/) - Latest test execution results
- 📈 [Coverage Reports](/coverage/) - Code coverage analysis