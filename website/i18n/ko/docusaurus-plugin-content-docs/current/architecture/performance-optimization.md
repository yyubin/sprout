# 📊성능 테스트 및 최적화

Sprout 서버의 성능 특성을 파악하고 병목 지점을 개선한 전체 과정을 다룹니다. 초기 벤치마킹부터 프로파일링 분석, 코드 리팩토링, 그리고 최종 성능 개선까지의 여정을 단계별로 정리합니다.

## 개요

Sprout는 Java NIO 기반의 고성능 웹 서버로, BIO(Blocking I/O)와 NIO(Non-blocking I/O)를 모두 지원하며 플랫폼 스레드와 가상 스레드 중 선택할 수 있습니다. 이러한 구조적 유연성 덕분에 다양한 조합에서의 성능 특성을 비교하고 최적화할 수 있었습니다.

### 테스트 환경

| 항목     | 사양                   |
| ------ | -------------------- |
| CPU    | 10 Cores             |
| Memory | 32GB                 |
| OS     | macOS Sequoia 15.6.1 |
| JDK    | OpenJDK 21           |
| Tool   | Gatling 3.x          |

### 서버 구성 조합

Sprout는 실행 모드와 스레드 타입을 조합하여 4가지 구성으로 동작합니다.

| I/O 모드 | 스레드 타입 | 설명 |
|---------|----------|------|
| **Hybrid (BIO)** | Platform Threads | HTTP는 BIO로 동작, 고정 크기 스레드 풀 사용 (150개) |
| **Hybrid (BIO)** | Virtual Threads | HTTP는 BIO로 동작, 가상 스레드 사용 |
| **NIO** | Platform Threads | HTTP가 NIO로 동작, 고정 크기 스레드 풀 사용 |
| **NIO** | Virtual Threads | HTTP가 NIO로 동작, 가상 스레드 사용 |

설정 예시
```yaml
server:
  execution-mode: nio # 실행 모드: nio 또는 hybrid
  thread-type: virtual  # 스레드 종류: virtual 또는 platform
  thread-pool-size: 150 # platform 스레드일 경우 사용할 스레드 풀 크기
```

## Phase 1: 초기 벤치마킹

### 벤치마크 시나리오

세 가지 시나리오로 서버의 특성을 측정했습니다

#### 1. HelloWorld 시나리오 (약 8,000 요청)
```java
@GetMapping("/hello")
public String hello() {
    return "Hello, World!";
}
```

가장 간단한 응답으로 순수 서버 성능을 측정합니다.

#### 2. CPU Intensive 시나리오 (약 2,000 요청)
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

CPU 바운드 작업에서의 서버 처리 능력을 측정합니다.

#### 3. Latency 시나리오 (약 20,000 요청)
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

I/O 블로킹이 빈번한 상황을 시뮬레이션합니다.

### 초기 벤치마킹 결과

| 조합                | HelloWorld | CPU   | Latency | 특성 요약                |
| ----------------- | ---------- | ----- | ------- | -------------------- |
| Hybrid + Platform | 84%        | 53%   | 95.6%   | 안정적이나 Warm-up 의존적    |
| Hybrid + Virtual  | 87%        | 47%   | 94.7%   | 초반 오버헤드 있으나 응답 빠름    |
| NIO + Platform    | 82%        | 45%   | 93.4%   | 초기 Selector 병목 발생    |
| NIO + Virtual     | 69%        | 64.9% | 92.2%   | CPU 부하에선 최적, I/O엔 부적 |

### 주요 발견 사항

#### 1. Warm-up 문제
모든 조합에서 초반 5-10초 구간에 요청 실패(KO)가 집중되고 이후 안정화되는 패턴이 관찰되었습니다. 그래프를 보면 특정 시점(약 300건의 요청 처리 후)부터 성능이 급증하는데, 이는 JIT 컴파일러가 "hot" 상태로 전환된 시점을 의미합니다.

**원인 분석**
- JIT 컴파일이 완료되기 전까지 인터프리터 모드로 실행되어 느림
- 특히 NIO 구조는 Selector, Channel, ByteBuffer 등 복잡한 루프 때문에 JIT threshold 도달에 더 많은 반복 필요
- BIO는 단순 socket read/write라 빨리 최적화 가능

#### 2. NIO + Virtual Thread의 특이한 패턴
HelloWorld 시나리오에서는 가장 낮은 성공률(69%)을 보였지만, CPU Intensive 시나리오에서는 가장 높은 성공률(64.9%)을 기록했습니다.

**해석**
- CPU 집약적 작업: NIO의 이벤트 분산과 Virtual Thread의 가벼움이 시너지를 냄
- I/O 지연 작업: 의도적 지연이 NIO의 비동기 처리와 맞지 않아 오히려 오버헤드 발생

#### 3. Hybrid + Virtual Thread의 안정성
대부분의 시나리오에서 가장 균형잡힌 성능을 보여줬습니다. BIO의 단순함과 Virtual Thread의 경량 특성이 잘 조합되어 안정적인 결과를 제공합니다.

## Phase 2: 병목 지점 분석

초기 벤치마킹에서 Warm-up 문제를 확인한 후, 정확한 병목 지점을 찾기 위해 프로파일링 도구를 활용했습니다.

### async-profiler 분석

`async-profiler`를 사용하여 CPU, 메모리 할당, Wall-clock 프로파일링을 진행했습니다.

```bash
# async-profiler 실행 스크립트
env DYLD_LIBRARY_PATH=$ASYNC_PROFILER_HOME/lib $ASPROF \
  -d 30 -e cpu -o flamegraph -f cpu-flamegraph.html $PID

env DYLD_LIBRARY_PATH=$ASYNC_PROFILER_HOME/lib $ASPROF \
  -d 30 -e alloc -o flamegraph -f alloc-flamegraph.html $PID

env DYLD_LIBRARY_PATH=$ASYNC_PROFILER_HOME/lib $ASPROF \
  -d 30 -e wall -o flamegraph -f wall-flamegraph.html $PID
```

#### CPU 프로파일 결과
- **JIT 컴파일**: `C2Compiler::compile_method`, `compiler_thread_loop` 등이 가장 많은 CPU 시간 소비
- **JMX 오버헤드**: `DefaultMBeanServerInterceptor.getAttribute`가 상당한 CPU 사용
- **네트워크 처리**: `DefaultConnectionManager.acceptConnection`에서 대기 관련 함수(`__psynch_cvwait`) 발견

#### 메모리 할당 프로파일 결과
가장 중요한 발견이었습니다

**주요 할당 지점:**
1. **HTTP 요청 파싱** (약 18%): `HttpHeaderParser.parse` → `ByteBuffer.allocate`
2. **HTTP 응답 버퍼 생성** (약 32%): `HttpUtils.createResponseBuffer`
3. **요청 라우팅/필터링**: `FilterChain.doFilter`, `HandlerMappingImpl.findHandler`
4. **문자열 파싱/정규식**: `Pattern.matcher`, `Pattern.split`

**총평: 전체 메모리 할당의 약 50%가 ByteBuffer 생성에 사용되고 있었습니다.**

#### Wall-clock 프로파일 결과
전체 실행 시간 중 95% 이상이 스레드의 대기(Waiting) 또는 휴면(Idle) 상태였습니다. 이는 정상적인 패턴으로, 실제 작업 시에는 앞서 발견한 병목 지점들이 문제가 됩니다.

### JMC(JDK Mission Control) 분석

```bash
# JVM 옵션으로 JFR 기록 활성화
-XX:StartFlightRecording=filename=jit-profile/recording.jfr,duration=300s,settings=profile
-XX:+UnlockDiagnosticVMOptions
-XX:+LogCompilation
-XX:LogFile=jit-profile/hotspot_%p.log
-XX:+PrintInlining
-XX:+PrintCompilation
```

#### JIT 컴파일 시간 분석
가장 긴 컴파일 시간(약 4.75ms)을 소비한 메서드들
1. `HttpUtils.readRawRequest` - HTTP 요청 읽기
2. `ClassLoader` 관련 메서드 - 클래스 로딩
3. `ObjectOutputStream.writeOrdinaryObject` - 객체 직렬화

#### GC 분석
```
Young Collection Total Time: 55.848 ms (123회)
Old Collection Total Time: 25.299 ms (1회)
Total GC Time: 81.146 ms
전체 실행 시간 대비 GC 비율: 0.054%
```

GC 부하는 거의 없는 수준이었지만, ByteBuffer 할당 문제를 해결하면 더 개선될 여지가 있었습니다.

### JITWatch 분석

```bash
# 상세한 JIT 로그 생성
-XX:+LogCompilation
-XX:+PrintInlining
-XX:+PrintAssembly
```

#### HttpUtils.readRawRequest 분석
JITWatch를 통해 정확한 문제점을 발견했습니다

**발견된 문제**
1. **"callee is too large"**: 메서드 크기가 JIT 컴파일러의 인라이닝 한계(~325 바이트) 초과
2. **"unpredictable branch"**: 분기 예측률 50% (chunked vs content-length)
3. **"callee uses too much stack"**: `new String(bytes, UTF_8)` 반복 생성으로 스택 압박

```java
// 문제가 있던 원본 코드 (93줄, 약 450 바이트코드)
public static String readRawRequest(ByteBuffer initial, InputStream in) throws IOException {
    StringBuilder sb = new StringBuilder();

    // ... 헤더 읽기 로직 ...

    // 분기 예측 실패 원인
    if (chunked) {
        bodyStart += readChunkedBody(bin);
    } else if (contentLength > -1) {
        // content-length 처리
    }

    return headers + "\r\n\r\n" + bodyStart;
}
```

## Phase 3: 성능 개선 1차 - ByteBufferPool 도입

메모리 할당 문제를 해결하기 위해 ByteBuffer 풀링을 도입했습니다.

### ByteBufferPool 구현

```java
@Component
public class ByteBufferPool implements InfrastructureBean {
    // 자주 쓰이는 버퍼 크기 사전 정의
    public static final int SMALL_BUFFER_SIZE = 2048;   // 2KB → 프로토콜 탐지용
    public static final int MEDIUM_BUFFER_SIZE = 8192;  // 8KB → 일반 요청 읽기용
    public static final int LARGE_BUFFER_SIZE = 32768;  // 32KB → 대용량 응답용

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

### 적용 결과

**성능 향상**
- 성공률: 67% → 81% (14% 향상)
- ByteBuffer 할당: 전체 할당의 50% → 거의 0%로 감소

**메모리 프로파일 변화**
- `ByteBuffer.allocate` 관련 스택이 거의 사라짐
- GC 부담 감소로 JMX 오버헤드도 함께 감소
- CPU 프로파일에서 JMX 관련 CPU 사용량 완전 제거

## Phase 4: 성능 개선 2차 - JIT 친화적 코드 리팩토링

JITWatch 분석 결과를 바탕으로 `HttpUtils.readRawRequest` 메서드를 리팩토링했습니다.

### 리팩토링 원칙

1. **메서드 분리**: 단일 책임 원칙 + JIT 인라이닝 최적화 (< 325 바이트)
2. **조기 리턴**: 빈도 높은 케이스 우선 처리로 분기 예측률 향상
3. **제로 카피**: 불필요한 String 생성 제거
4. **BufferedInputStream 재사용**: 데이터 손실 방지

### 메서드 분리

#### Before: 단일 거대 메서드 (93줄, ~450 바이트코드)
```java
public static String readRawRequest(ByteBuffer initial, InputStream in) {
    // 43줄의 복잡한 로직
    // - 헤더 읽기
    // - 파싱
    // - 바디 읽기 (content-length/chunked)
}
```

#### After: 3개의 작은 메서드로 분리
```java
// 1. 조합 메서드 (30줄, ~200 바이트코드)
public static String readRawRequest(ByteBuffer initial, InputStream in) throws IOException {
    BufferedInputStream bin = new BufferedInputStream(in);
    String headerPart = readHeadersFromStream(initial, bin);

    int headerEnd = headerPart.indexOf("\r\n\r\n");
    if (headerEnd < 0) return headerPart;

    String headers = headerPart.substring(0, headerEnd);
    String bodyStart = headerPart.substring(headerEnd + 4);

    // 조기 리턴 패턴 (빈도 순서)
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

// 2. 헤더 읽기 전용 (18줄, ~120 바이트코드)
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

// 3. 바디 읽기 전용 (10줄, ~80 바이트코드)
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

**개선 효과:**
- 모든 메서드가 325 바이트 이하가 되어 C2 컴파일러 인라이닝 대상이 됨
- 메서드별 책임이 명확해져 테스트와 유지보수 용이

### 조기 리턴 패턴

#### Before: 복잡한 if-else 중첩 (분기 예측률 50%)
```java
if (chunked) {
    // chunked 처리 (실제로는 10% 미만)
} else if (contentLength > -1) {
    // content-length 처리 (실제로는 80% 이상)
}
```

#### After: 빈도 순서대로 조기 리턴 (예상 분기 예측률 80%+)
```java
// 1. Content-Length > 0 (80%+ 케이스) → 즉시 리턴
int contentLength = parseContentLength(headers);
if (contentLength > 0) {
    String body = readBodyWithContentLength(bin, contentLength, bodyStart);
    return headers + "\r\n\r\n" + body;
}

// 2. Content-Length == 0 (5% 케이스) → 즉시 리턴
if (contentLength == 0) {
    return headers + "\r\n\r\n" + bodyStart;
}

// 3. Chunked (10% 미만) → 즉시 리턴
if (isChunked(headers)) {
    String chunkedBody = readChunkedBody(bin);
    return headers + "\r\n\r\n" + bodyStart + chunkedBody;
}
```

**개선 효과**
- CPU 분기 예측 성공률이 50% → 80%로 향상
- 파이프라인 플러시 빈도 감소

### 헤더 파싱 최적화

#### Before: split() + toLowerCase() (요청당 ~40개 객체 생성)
```java
private static int parseContentLength(String headers) {
    for (String line : headers.split("\r\n")) {  // String[] 배열 생성
        if (line.toLowerCase().startsWith("content-length:")) {  // String 복사
            return Integer.parseInt(line.split(":")[1].trim());  // 또 배열 생성
        }
    }
    return -1;
}
```

#### After: indexOf() + 직접 문자 비교 (0~1개 객체 생성)
```java
private static int parseContentLength(String headers) {
    int pos = 0;
    int headersLength = headers.length();

    while (pos < headersLength) {
        int lineEnd = headers.indexOf("\r\n", pos);
        if (lineEnd < 0) lineEnd = headersLength;

        // "content-length:" 대소문자 무시 비교 (15자)
        if (regionMatchesIgnoreCase(headers, pos, "content-length:", 15)) {
            int colonIdx = headers.indexOf(':', pos);
            if (colonIdx < 0 || colonIdx >= lineEnd) {
                pos = lineEnd + 2;
                continue;
            }

            // 값 추출 (trim 없이 직접 처리)
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

**개선 효과**
- 메모리 할당: 요청당 ~40개 → 0~1개 객체 (97.5% 감소)
- String[] 배열 생성 제거
- toLowerCase() 복사 제거
- split(":") 배열 생성 제거

### 리팩토링 2차 결과

#### Full Warm-up 테스트 (약 120,000 요청)
- 성공률: 99.73% → 99.84% (0.11% 향상)
- JIT 컴파일 시간: `readRawRequest` 병목 지점 완전히 제거
- JITWatch 제안 사항: 0개 (모든 최적화 완료)

#### GC 영향 분석
```
Young GC Count: 123 → 127 (+3.2%)
Young GC Total Time: 55.848 ms → 62.710 ms (+12.3%)
Average GC Time: 0.454 ms → 0.494 ms (+8.8%)
```

GC 압력이 약간 증가했지만 무시 가능한 수준입니다. `substring()` 사용으로 인한 단기 객체 증가가 원인으로 추정됩니다.

## 최종 성능 개선 결과

### Partial Warm-up 테스트 (약 8,000 요청)

실제 운영 환경에 가까운 짧은 Warm-up 시나리오로 최종 성능을 측정했습니다.

| I/O 모델               | Executor 타입      | 기존 성공률(%) | 개선 후 성공률(%) | 개선폭(Δ%)    |
| -------------------- | ---------------- | --------- | ----------- | ---------- |
| **Hybrid (BIO)** | Platform Threads | 83.95     | 96.54       | **+12.59** |
| **Hybrid (BIO)** | Virtual Threads  | 88.00     | 96.25       | **+8.25**  |
| **NIO**              | Platform Threads | 72.79     | 96.28       | **+23.49** |
| **NIO**              | Virtual Threads  | 69.00     | 98.00       | **+29.00** |

### 주요 개선 사항

#### 1. Warm-up 시간 단축
- 안정화까지 소요 시간: 약 60초 → 약 20초
- JIT 컴파일 병목 제거로 초기 응답 실패율 대폭 감소

#### 2. NIO 구조의 극적인 개선
- NIO + Virtual Thread: 69% → 98% (**+29%**)
- NIO + Platform Thread: 72.79% → 96.28% (**+23.49%**)
- NIO 구조가 cold-start에 가장 민감했으나, 개선 후 가장 큰 폭의 향상

#### 3. 메모리 효율 개선
- ByteBuffer 할당: 전체 할당의 50% → 거의 0%
- GC 부담 감소로 안정적인 응답 시간 유지
- JMX 오버헤드 완전 제거

#### 4. CPU 효율 향상
- JIT 컴파일 시간 감소
- 분기 예측 성공률 향상
- 메서드 인라이닝 성공

### 권장 구성

테스트 결과를 바탕으로 사용 사례별 권장 구성은 다음과 같습니다

#### 높은 처리량이 필요한 API 서버
```yaml
server:
  execution-mode: hybrid
  thread-type: virtual
```
**특징**
- 간단한 코드로 높은 처리량
- 자동 백프레셔
- 가상 스레드와 잘 작동

#### 최대 확장성이 필요한 서버 (연결 집약적)
```yaml
server:
  execution-mode: nio
  thread-type: virtual
```
**특징**
- cold-start에서 최고 개선폭 (+29%)
- 안정화 후 가장 높은 성공률 (98%)
- 수천 개의 동시 연결 처리 가능

#### 안정성 우선 서버
```yaml
server:
  execution-mode: hybrid
  thread-type: platform
  thread-pool-size: 200
```
**특징**
- 가장 예측 가능한 동작
- 모든 시나리오에서 균형잡힌 성능
- 레거시 호환성

## 추가 최적화 고려사항

### 1. JVM 옵션 튜닝

#### Tiered Compilation
```bash
# C2 컴파일러만 사용 (최대 성능, 느린 시작)
-XX:TieredStopAtLevel=4

# C1 컴파일러만 사용 (빠른 시작, 낮은 피크 성능)
-XX:TieredStopAtLevel=1
```

#### JIT 컴파일 임계값 조정
```bash
# 더 빠른 JIT 컴파일 트리거
-XX:CompileThreshold=5000

# 인라이닝 한계 증가
-XX:MaxInlineSize=500
-XX:FreqInlineSize=500
```

### 2. 추가 풀링 대상

현재 ByteBuffer만 풀링하고 있지만, 다음 객체들도 풀링 가능합니다

```java
// HttpRequest/HttpResponse 객체 풀링
@Component
public class HttpObjectPool {
    private final Queue<HttpRequest<?>> requestPool = new ConcurrentLinkedQueue<>();
    private final Queue<HttpResponse> responsePool = new ConcurrentLinkedQueue<>();

    public HttpRequest<?> borrowRequest() { /* ... */ }
    public void returnRequest(HttpRequest<?> request) { /* ... */ }
}
```

### 3. 라우팅 캐시 개선

현재 간단한 캐싱만 적용되어 있습니다

```java
// 현재 구현
private final Map<String, PathPattern> pathPatterns = new ConcurrentHashMap<>();

// 개선 가능 방향: LRU 캐시
private final Cache<String, RequestMappingInfo> routingCache =
    CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .build();
```

### 4. GC 튜닝

```bash
# G1 GC 최적화
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:G1HeapRegionSize=16m

# 또는 저지연 GC (Shenandoah, ZGC)
-XX:+UseShenandoahGC
# -XX:+UseZGC
```

## 성능 모니터링

### 프로파일링 스크립트

프로젝트에 포함된 성능 테스트 스크립트를 활용할 수 있습니다

```bash
# async-profiler를 사용한 프로파일링
./run-performance-tests.sh

# JIT 분석을 위한 상세 로그
./jit-benchmark.sh
```

### Wireshark를 통한 네트워크 분석

```bash
# HTTP 트래픽 캡처 및 분석
./wireshark-analysis.sh
```

## 결론

Sprout 서버는 체계적인 성능 분석과 개선 작업을 통해 다음과 같은 성과를 달성했습니다

### 주요 성과
1. **Warm-up 시간 67% 단축**: 60초 → 20초
2. **초기 성공률 최대 29% 향상**: NIO + VT 조합에서 69% → 98%
3. **메모리 효율 97.5% 개선**: 요청당 객체 생성 40개 → 0~1개
4. **JIT 컴파일 병목 완전 제거**: 주요 hot path 메서드 인라이닝 성공

### 핵심 교훈
1. **프로파일링의 중요성**: async-profiler, JMC, JITWatch를 조합하여 정확한 병목 지점 파악
2. **JIT 친화적 코딩**: 메서드 크기, 분기 예측, 조기 리턴 패턴이 성능에 큰 영향
3. **객체 풀링**: GC 언어에서 메모리 주도권을 잡는 효과적인 방법
4. **구조적 유연성**: 다양한 I/O 모델과 스레드 전략으로 사용 사례별 최적화 가능

### 운영 권장사항
- **일반적인 웹 애플리케이션**: Hybrid + Virtual Thread
- **고부하 실시간 서비스**: NIO + Virtual Thread
- **안정성 우선**: Hybrid + Platform Thread

---

위 성능 개선점에 대한 부가적인 사항은 제 개인 블로그에 기재되어 있습니다.
- [블로그 바로가기](https://velog.io/@cassidy/posts)

- [서버 성능 및 안정성 테스트](https://velog.io/@cassidy/Sprout-5.-%EC%9E%90%EC%B2%B4-%EC%A0%9C%EC%9E%91-%EC%84%9C%EB%B2%84-%EC%84%B1%EB%8A%A5-%EB%B0%8F-%EC%95%88%EC%A0%95%EC%84%B1-%ED%85%8C%EC%8A%A4%ED%8A%B8)

- [async-profiler를 활용한 성능 개선](https://velog.io/@cassidy/Sprout-8.-async-profiler%EB%A1%9C-%EC%9E%90%EC%B2%B4-%EC%A0%9C%EC%9E%91-%EC%84%9C%EB%B2%84-%EC%84%B1%EB%8A%A5-%EA%B0%9C%EC%84%A0)

- [JMC, JITWatch로 병목 분석](https://velog.io/@cassidy/Sprout-9.-JMC-JITWatch%EB%A1%9C-%EC%A0%9C%EC%9E%91-%EC%84%9C%EB%B2%84-JIT-%EC%BB%B4%ED%8C%8C%EC%9D%BC-%EB%B6%84%EC%84%9D)

- [JIT 최적화를 통한 성능 개선](https://velog.io/@cassidy/Sprout-10.-%EC%9E%90%EC%B2%B4-%EC%A0%9C%EC%9E%91-%EC%84%9C%EB%B2%84-%EC%84%B1%EB%8A%A5-%EA%B0%9C%EC%84%A0-%EB%A6%AC%ED%8C%A9%ED%86%A0%EB%A7%81)