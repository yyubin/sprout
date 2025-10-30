# 📊 Performance Testing and Optimization

This document covers the entire journey of understanding Sprout server's performance characteristics and improving bottlenecks. From initial benchmarking through profiling analysis, code refactoring, to final performance improvements are organized step by step.

## Overview

Sprout is a high-performance web server based on Java NIO that supports both BIO (Blocking I/O) and NIO (Non-blocking I/O), with the option to choose between platform threads and virtual threads. This structural flexibility enabled comparison and optimization of performance characteristics across various combinations.

### Test Environment

| Item   | Specification         |
| ------ | -------------------- |
| CPU    | 10 Cores             |
| Memory | 32GB                 |
| OS     | macOS Sequoia 15.6.1 |
| JDK    | OpenJDK 21           |
| Tool   | Gatling 3.x          |

### Server Configuration Combinations

Sprout operates in 4 different configurations by combining execution mode and thread type:

| I/O Mode | Thread Type | Description |
|---------|----------|------|
| **Hybrid (BIO)** | Platform Threads | HTTP operates with BIO, uses fixed-size thread pool (150) |
| **Hybrid (BIO)** | Virtual Threads | HTTP operates with BIO, uses virtual threads |
| **NIO** | Platform Threads | HTTP operates with NIO, uses fixed-size thread pool |
| **NIO** | Virtual Threads | HTTP operates with NIO, uses virtual threads |

Configuration example:
```yaml
server:
  execution-mode: nio # Execution mode: nio or hybrid
  thread-type: virtual  # Thread type: virtual or platform
  thread-pool-size: 150 # Thread pool size for platform threads
```

## Phase 1: Initial Benchmarking

### Benchmark Scenarios

Server characteristics were measured through three scenarios:

#### 1. HelloWorld Scenario (~8,000 requests)
```java
@GetMapping("/hello")
public String hello() {
    return "Hello, World!";
}
```

Measures pure server performance with the simplest response.

#### 2. CPU Intensive Scenario (~2,000 requests)
```java
@GetMapping("/cpu")
public String cpu(@RequestParam(required = false, defaultValue = "35") String n) {
    int num = Integer.parseInt(n);
    long result = fibonacci(num);
    return "Fibonacci(" + num + ") = " + result;
}

@GetMapping("/cpu-heavy")
public String cpuHeavy(@RequestParam(required = false, defaultValue = "10000") String limit) {
    int max = Integer.parseInt(limit);
    int primeCount = countPrimes(max);
    return "Primes up to " + max + ": " + primeCount;
}
```

Measures server processing capability under CPU-bound workload.

#### 3. Latency Scenario (~20,000 requests)
```java
@GetMapping("/latency")
public String latency(@RequestParam(required = false, defaultValue = "100") String ms) {
    int delay = Integer.parseInt(ms);
    try {
        Thread.sleep(delay);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return "Interrupted";
    }
    return "Delayed response after " + delay + "ms";
}
```

Simulates situations with frequent I/O blocking.

### Initial Benchmarking Results

| Configuration     | HelloWorld | CPU   | Latency | Summary                |
| ----------------- | ---------- | ----- | ------- | -------------------- |
| Hybrid + Platform | 84%        | 53%   | 95.6%   | Stable but warm-up dependent |
| Hybrid + Virtual  | 87%        | 47%   | 94.7%   | Initial overhead but fast response |
| NIO + Platform    | 82%        | 45%   | 93.4%   | Initial Selector bottleneck |
| NIO + Virtual     | 69%        | 64.9% | 92.2%   | Optimal for CPU load, unsuitable for I/O |

### Key Findings

#### 1. Warm-up Issue
All combinations showed request failures (KO) concentrated in the initial 5-10 seconds, followed by stabilization. Looking at the graphs, performance surges after a certain point (after processing ~300 requests), indicating when the JIT compiler transitions to a "hot" state.

**Root Cause Analysis:**
- Runs in interpreter mode until JIT compilation completes, causing slowness
- NIO structure requires more iterations to reach JIT threshold due to complex loops with Selector, Channel, ByteBuffer
- BIO can be optimized quickly due to simple socket read/write operations

#### 2. Unusual Pattern of NIO + Virtual Thread
Showed the lowest success rate (69%) in HelloWorld scenario but recorded the highest success rate (64.9%) in CPU Intensive scenario.

**Interpretation:**
- CPU-intensive tasks: NIO's event distribution and Virtual Thread's lightness create synergy
- I/O latency tasks: Intentional delays don't align with NIO's async processing, causing overhead

#### 3. Stability of Hybrid + Virtual Thread
Showed the most balanced performance across most scenarios. BIO's simplicity combined well with Virtual Thread's lightweight characteristics to provide stable results.

## Phase 2: Bottleneck Analysis

After identifying warm-up issues in initial benchmarking, profiling tools were used to find exact bottlenecks.

### async-profiler Analysis

CPU, memory allocation, and wall-clock profiling were conducted using `async-profiler`.

```bash
# async-profiler execution script
env DYLD_LIBRARY_PATH=$ASYNC_PROFILER_HOME/lib $ASPROF \
  -d 30 -e cpu -o flamegraph -f cpu-flamegraph.html $PID

env DYLD_LIBRARY_PATH=$ASYNC_PROFILER_HOME/lib $ASPROF \
  -d 30 -e alloc -o flamegraph -f alloc-flamegraph.html $PID

env DYLD_LIBRARY_PATH=$ASYNC_PROFILER_HOME/lib $ASPROF \
  -d 30 -e wall -o flamegraph -f wall-flamegraph.html $PID
```

#### CPU Profile Results
- **JIT Compilation**: `C2Compiler::compile_method`, `compiler_thread_loop` consumed most CPU time
- **JMX Overhead**: `DefaultMBeanServerInterceptor.getAttribute` consumed significant CPU
- **Network Processing**: Wait-related functions (`__psynch_cvwait`) found in `DefaultConnectionManager.acceptConnection`

#### Memory Allocation Profile Results
The most important discovery:

**Major Allocation Points:**
1. **HTTP Request Parsing** (~18%): `HttpHeaderParser.parse` → `ByteBuffer.allocate`
2. **HTTP Response Buffer Creation** (~32%): `HttpUtils.createResponseBuffer`
3. **Request Routing/Filtering**: `FilterChain.doFilter`, `HandlerMappingImpl.findHandler`
4. **String Parsing/Regex**: `Pattern.matcher`, `Pattern.split`

**Summary: Approximately 50% of total memory allocation was used for ByteBuffer creation.**

#### Wall-clock Profile Results
Over 95% of total execution time was in thread waiting or idle state. This is a normal pattern, with the previously discovered bottlenecks becoming issues during actual work.

### JMC (JDK Mission Control) Analysis

```bash
# Enable JFR recording with JVM options
-XX:StartFlightRecording=filename=jit-profile/recording.jfr,duration=300s,settings=profile
-XX:+UnlockDiagnosticVMOptions
-XX:+LogCompilation
-XX:LogFile=jit-profile/hotspot_%p.log
-XX:+PrintInlining
-XX:+PrintCompilation
```

#### JIT Compilation Time Analysis
Methods consuming the longest compilation time (~4.75ms):
1. `HttpUtils.readRawRequest` - HTTP request reading
2. `ClassLoader` related methods - Class loading
3. `ObjectOutputStream.writeOrdinaryObject` - Object serialization

#### GC Analysis
```
Young Collection Total Time: 55.848 ms (123 times)
Old Collection Total Time: 25.299 ms (1 time)
Total GC Time: 81.146 ms
GC ratio compared to total execution time: 0.054%
```

GC load was negligible, but there was room for improvement by solving ByteBuffer allocation issues.

### JITWatch Analysis

```bash
# Generate detailed JIT logs
-XX:+LogCompilation
-XX:+PrintInlining
-XX:+PrintAssembly
```

#### HttpUtils.readRawRequest Analysis
Exact problems were discovered through JITWatch:

**Discovered Issues:**
1. **"callee is too large"**: Method size exceeds JIT compiler's inlining limit (~325 bytes)
2. **"unpredictable branch"**: 50% branch prediction rate (chunked vs content-length)
3. **"callee uses too much stack"**: Stack pressure from repeated `new String(bytes, UTF_8)` creation

```java
// Problematic original code (93 lines, ~450 bytecode)
public static String readRawRequest(ByteBuffer initial, InputStream in) throws IOException {
    StringBuilder sb = new StringBuilder();

    // ... header reading logic ...

    // Cause of branch prediction failure
    if (chunked) {
        bodyStart += readChunkedBody(bin);
    } else if (contentLength > -1) {
        // content-length processing
    }

    return headers + "\r\n\r\n" + bodyStart;
}
```

## Phase 3: Performance Improvement 1st - ByteBufferPool Introduction

ByteBuffer pooling was introduced to solve memory allocation issues.

### ByteBufferPool Implementation

```java
@Component
public class ByteBufferPool implements InfrastructureBean {
    // Pre-defined commonly used buffer sizes
    public static final int SMALL_BUFFER_SIZE = 2048;   // 2KB → for protocol detection
    public static final int MEDIUM_BUFFER_SIZE = 8192;  // 8KB → for general request reading
    public static final int LARGE_BUFFER_SIZE = 32768;  // 32KB → for large responses

    private final ConcurrentHashMap<Integer, PoolConfig> pools;

    public ByteBufferPool() {
        this.pools = new ConcurrentHashMap<>();
        initializePool(SMALL_BUFFER_SIZE, 500);
        initializePool(MEDIUM_BUFFER_SIZE, 500);
        initializePool(LARGE_BUFFER_SIZE, 100);
    }

    public ByteBuffer acquire(int size) {
        int poolSize = findPoolSize(size);
        PoolConfig config = pools.get(poolSize);

        ByteBuffer buffer = config.pool.poll();
        if (buffer != null) {
            buffer.clear();
            return buffer;
        }

        return allocateBuffer(poolSize);
    }

    public void release(ByteBuffer buffer) {
        if (buffer == null) return;

        PoolConfig config = pools.get(buffer.capacity());
        if (config == null) return;

        if (config.pool.size() >= config.maxPoolSize) return;

        buffer.clear();
        config.pool.offer(buffer);
    }
}
```

### Application Results

**Performance Improvement:**
- Success rate: 67% → 81% (14% improvement)
- ByteBuffer allocation: 50% of total allocation → nearly 0%

**Memory Profile Changes:**
- ByteBuffer.allocate related stacks almost disappeared
- JMX overhead also reduced due to decreased GC burden
- JMX-related CPU usage completely removed from CPU profile

## Phase 4: Performance Improvement 2nd - JIT-Friendly Code Refactoring

The `HttpUtils.readRawRequest` method was refactored based on JITWatch analysis results.

### Refactoring Principles

1. **Method Separation**: Single Responsibility Principle + JIT inlining optimization (< 325 bytes)
2. **Early Return**: Process frequent cases first to improve branch prediction
3. **Zero Copy**: Remove unnecessary String creation
4. **BufferedInputStream Reuse**: Prevent data loss

### Method Separation

#### Before: Single Large Method (93 lines, ~450 bytecode)
```java
public static String readRawRequest(ByteBuffer initial, InputStream in) {
    // 43 lines of complex logic
    // - Header reading
    // - Parsing
    // - Body reading (content-length/chunked)
}
```

#### After: Split into 3 Small Methods
```java
// 1. Composition method (30 lines, ~200 bytecode)
public static String readRawRequest(ByteBuffer initial, InputStream in) throws IOException {
    BufferedInputStream bin = new BufferedInputStream(in);
    String headerPart = readHeadersFromStream(initial, bin);

    int headerEnd = headerPart.indexOf("\r\n\r\n");
    if (headerEnd < 0) return headerPart;

    String headers = headerPart.substring(0, headerEnd);
    String bodyStart = headerPart.substring(headerEnd + 4);

    // Early return pattern (frequency order)
    int contentLength = parseContentLength(headers);
    if (contentLength > 0) {
        String body = readBodyWithContentLength(bin, contentLength, bodyStart);
        return headers + "\r\n\r\n" + body;
    }

    if (contentLength == 0) {
        return headers + "\r\n\r\n" + bodyStart;
    }

    if (isChunked(headers)) {
        String chunkedBody = readChunkedBody(bin);
        return headers + "\r\n\r\n" + bodyStart + chunkedBody;
    }

    return headers + "\r\n\r\n" + bodyStart;
}

// 2. Header reading only (18 lines, ~120 bytecode)
private static String readHeadersFromStream(ByteBuffer initial, BufferedInputStream bin) {
    StringBuilder sb = new StringBuilder();

    if (initial != null && initial.hasRemaining()) {
        byte[] arr = new byte[initial.remaining()];
        initial.get(arr);
        sb.append(new String(arr, StandardCharsets.UTF_8));
    }

    while (!sb.toString().contains("\r\n\r\n")) {
        int ch = bin.read();
        if (ch == -1) break;
        sb.append((char) ch);
    }

    return sb.toString();
}

// 3. Body reading only (10 lines, ~80 bytecode)
private static String readBodyWithContentLength(BufferedInputStream bin,
                                                int contentLength,
                                                String bodyStart) {
    int alreadyRead = bodyStart.getBytes(StandardCharsets.UTF_8).length;
    int remaining = contentLength - alreadyRead;

    if (remaining <= 0) return bodyStart;

    byte[] bodyBytes = bin.readNBytes(remaining);
    return bodyStart + new String(bodyBytes, StandardCharsets.UTF_8);
}
```

**Improvement Effect:**
- All methods became under 325 bytes, making them C2 compiler inlining candidates
- Clear responsibility per method makes testing and maintenance easier

### Early Return Pattern

#### Before: Complex if-else Nesting (50% branch prediction rate)
```java
if (chunked) {
    // chunked processing (actually less than 10%)
} else if (contentLength > -1) {
    // content-length processing (actually over 80%)
}
```

#### After: Early Return by Frequency Order (expected 80%+ branch prediction rate)
```java
// 1. Content-Length > 0 (80%+ case) → immediate return
int contentLength = parseContentLength(headers);
if (contentLength > 0) {
    String body = readBodyWithContentLength(bin, contentLength, bodyStart);
    return headers + "\r\n\r\n" + body;
}

// 2. Content-Length == 0 (5% case) → immediate return
if (contentLength == 0) {
    return headers + "\r\n\r\n" + bodyStart;
}

// 3. Chunked (less than 10%) → immediate return
if (isChunked(headers)) {
    String chunkedBody = readChunkedBody(bin);
    return headers + "\r\n\r\n" + bodyStart + chunkedBody;
}
```

**Improvement Effect:**
- CPU branch prediction success rate improved from 50% → 80%
- Reduced pipeline flush frequency

### Header Parsing Optimization

#### Before: split() + toLowerCase() (~40 objects per request)
```java
private static int parseContentLength(String headers) {
    for (String line : headers.split("\r\n")) {  // String[] array creation
        if (line.toLowerCase().startsWith("content-length:")) {  // String copy
            return Integer.parseInt(line.split(":")[1].trim());  // more array creation
        }
    }
    return -1;
}
```

#### After: indexOf() + Direct Character Comparison (0-1 objects)
```java
private static int parseContentLength(String headers) {
    int pos = 0;
    int headersLength = headers.length();

    while (pos < headersLength) {
        int lineEnd = headers.indexOf("\r\n", pos);
        if (lineEnd < 0) lineEnd = headersLength;

        // Case-insensitive comparison of "content-length:" (15 chars)
        if (regionMatchesIgnoreCase(headers, pos, "content-length:", 15)) {
            int colonIdx = headers.indexOf(':', pos);
            if (colonIdx < 0 || colonIdx >= lineEnd) {
                pos = lineEnd + 2;
                continue;
            }

            // Value extraction (trim without creating new strings)
            int valueStart = colonIdx + 1;
            while (valueStart < lineEnd && headers.charAt(valueStart) == ' ') {
                valueStart++;
            }

            int valueEnd = lineEnd;
            while (valueEnd > valueStart && headers.charAt(valueEnd - 1) == ' ') {
                valueEnd--;
            }

            try {
                return Integer.parseInt(headers.substring(valueStart, valueEnd));
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        pos = lineEnd + 2;
    }
    return -1;
}
```

**Improvement Effect:**
- Memory allocation: ~40 → 0-1 objects per request (97.5% reduction)
- Eliminated String[] array creation
- Eliminated toLowerCase() copies
- Eliminated split(":") array creation

### 2nd Refactoring Results

#### Full Warm-up Test (~120,000 requests)
- Success rate: 99.73% → 99.84% (0.11% improvement)
- JIT compilation time: `readRawRequest` bottleneck completely removed
- JITWatch suggestions: 0 (all optimizations completed)

#### GC Impact Analysis
```
Young GC Count: 123 → 127 (+3.2%)
Young GC Total Time: 55.848 ms → 62.710 ms (+12.3%)
Average GC Time: 0.454 ms → 0.494 ms (+8.8%)
```

GC pressure slightly increased but at a negligible level. Presumed to be caused by short-lived object increase from `substring()` usage.

## Final Performance Improvement Results

### Partial Warm-up Test (~8,000 requests)

Final performance was measured with a short warm-up scenario close to actual operating environment.

| I/O Model          | Executor Type    | Initial Success(%) | Improved Success(%) | Improvement(Δ%) |
| -------------------- | ---------------- | --------- | ----------- | ---------- |
| **Hybrid (BIO)** | Platform Threads | 83.95     | 96.54       | **+12.59** |
| **Hybrid (BIO)** | Virtual Threads  | 88.00     | 96.25       | **+8.25**  |
| **NIO**              | Platform Threads | 72.79     | 96.28       | **+23.49** |
| **NIO**              | Virtual Threads  | 69.00     | 98.00       | **+29.00** |

### Key Improvements

#### 1. Warm-up Time Reduction
- Time to stabilization: ~60 seconds → ~20 seconds
- Drastic reduction in initial response failure rate due to JIT compilation bottleneck removal

#### 2. Dramatic Improvement in NIO Structure
- NIO + Virtual Thread: 69% → 98% (**+29%**)
- NIO + Platform Thread: 72.79% → 96.28% (**+23.49%**)
- NIO structure was most sensitive to cold-start but showed the greatest improvement

#### 3. Memory Efficiency Improvement
- ByteBuffer allocation: 50% of total allocation → nearly 0%
- Maintained stable response times due to reduced GC burden
- Completely eliminated JMX overhead

#### 4. CPU Efficiency Improvement
- Reduced JIT compilation time
- Improved branch prediction success rate
- Successful method inlining

### Recommended Configurations

Based on test results, recommended configurations by use case:

#### High-Throughput API Server
```yaml
server:
  execution-mode: hybrid
  thread-type: virtual
```
**Features:**
- High throughput with simple code
- Automatic backpressure
- Works well with virtual threads

#### Maximum Scalability Server (Connection-Intensive)
```yaml
server:
  execution-mode: nio
  thread-type: virtual
```
**Features:**
- Highest improvement in cold-start (+29%)
- Highest success rate after stabilization (98%)
- Can handle thousands of concurrent connections

#### Stability-First Server
```yaml
server:
  execution-mode: hybrid
  thread-type: platform
  thread-pool-size: 200
```
**Features:**
- Most predictable behavior
- Balanced performance across all scenarios
- Legacy compatibility

## Additional Optimization Considerations

### 1. JVM Option Tuning

#### Tiered Compilation
```bash
# Use C2 compiler only (maximum performance, slow start)
-XX:TieredStopAtLevel=4

# Use C1 compiler only (fast start, lower peak performance)
-XX:TieredStopAtLevel=1
```

#### JIT Compilation Threshold Adjustment
```bash
# Faster JIT compilation trigger
-XX:CompileThreshold=5000

# Increase inlining limits
-XX:MaxInlineSize=500
-XX:FreqInlineSize=500
```

### 2. Additional Pooling Targets

Currently only ByteBuffer is pooled, but these objects can also be pooled:

```java
// HttpRequest/HttpResponse object pooling
@Component
public class HttpObjectPool {
    private final Queue<HttpRequest<?>> requestPool = new ConcurrentLinkedQueue<>();
    private final Queue<HttpResponse> responsePool = new ConcurrentLinkedQueue<>();

    public HttpRequest<?> borrowRequest() { /* ... */ }
    public void returnRequest(HttpRequest<?> request) { /* ... */ }
}
```

### 3. Routing Cache Improvement

Currently only simple caching is applied:

```java
// Current implementation
private final Map<String, PathPattern> pathPatterns = new ConcurrentHashMap<>();

// Improvement direction: LRU cache
private final Cache<String, RequestMappingInfo> routingCache =
    CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .build();
```

### 4. GC Tuning

```bash
# G1 GC optimization
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:G1HeapRegionSize=16m

# Or low-latency GC (Shenandoah, ZGC)
-XX:+UseShenandoahGC
# -XX:+UseZGC
```

## Performance Monitoring

### Profiling Scripts

Performance test scripts included in the project can be utilized:

```bash
# Profiling with async-profiler
./run-performance-tests.sh

# Detailed logs for JIT analysis
./jit-benchmark.sh
```

### Network Analysis with Wireshark

```bash
# Capture and analyze HTTP traffic
./wireshark-analysis.sh
```

## Conclusion

Sprout server achieved the following results through systematic performance analysis and improvement work:

### Key Achievements
1. **67% Warm-up Time Reduction**: 60 seconds → 20 seconds
2. **Up to 29% Initial Success Rate Improvement**: NIO + VT combination 69% → 98%
3. **97.5% Memory Efficiency Improvement**: Object creation per request 40 → 0-1
4. **Complete JIT Compilation Bottleneck Removal**: Successful inlining of major hot path methods

### Core Lessons
1. **Importance of Profiling**: Accurate bottleneck identification by combining async-profiler, JMC, JITWatch
2. **JIT-Friendly Coding**: Method size, branch prediction, early return patterns significantly impact performance
3. **Object Pooling**: Effective way to maintain memory control in GC languages
4. **Structural Flexibility**: Use case-specific optimization possible with various I/O models and thread strategies

### Operational Recommendations
- **General Web Applications**: Hybrid + Virtual Thread
- **High-Load Real-time Services**: NIO + Virtual Thread
- **Stability First**: Hybrid + Platform Thread

Sprout now provides competitive performance as a modern Java web server and will continue to evolve through ongoing optimization.
