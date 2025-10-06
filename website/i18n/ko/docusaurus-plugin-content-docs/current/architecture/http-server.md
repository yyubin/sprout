# 🌐 HTTP 서버

HTTP 서버는 Tomcat의 핵심 기능을 재현한 Sprout의 임베디드 서버 구현체입니다. Blocking I/O (BIO)와 Non-blocking I/O (NIO)를 모두 지원하며, 플랫폼 스레드 또는 가상 스레드 옵션을 제공하는 유연하고 설정 가능한 서버 인프라를 제공합니다.

## 개요

Sprout의 HTTP 서버는 다음을 제공합니다:
- **NIO 기반 이벤트 루프**: Java NIO Selector를 사용한 고성능 논블로킹 I/O
- **하이브리드 BIO/NIO 모드**: 프로토콜 핸들러별 유연한 I/O 전략 선택
- **가상 스레드 지원**: Java 21의 가상 스레드를 활용한 현대적인 동시성 처리
- **플랫폼 스레드 풀**: 호환성을 위한 전통적인 스레드 풀
- **프로토콜 감지**: HTTP/WebSocket 프로토콜 자동 식별
- **플러그인형 핸들러 아키텍처**: 확장 가능한 프로토콜 핸들러 시스템

## 서버 아키텍처

### 핵심 컴포넌트

HTTP 서버는 다음의 주요 컴포넌트로 구성됩니다:

- `HttpServer`: 라이프사이클 관리를 구현하는 메인 서버 파사드
- `ServerStrategy`: 플러그인형 서버 전략 인터페이스 (NIO 이벤트 루프)
- `ConnectionManager`: 연결 수락 및 프로토콜 라우팅
- `ProtocolDetector`: 초기 바이트로부터 프로토콜 식별
- `ProtocolHandler`: 프로토콜별 요청 처리
- `RequestExecutorService`: 스레드 관리 추상화

### 서버 초기화 프로세스

```java
public class SproutApplication {
    public static void run(Class<?> primarySource) throws Exception {
        // 1. 애플리케이션 컨텍스트 생성
        ApplicationContext ctx = new SproutApplicationContext(packages);
        ctx.refresh();

        // 2. HttpServer 빈 가져오기
        HttpServer server = ctx.getBean(HttpServer.class);

        // 3. 서버 시작
        int port = server.start(8080);
        System.out.println("Server started on port " + port);
    }
}
```

## 스레드 실행 모드

### 가상 스레드 모드 (기본값)

가상 스레드는 높은 처리량의 애플리케이션을 위한 경량 동시성을 제공합니다:

```java
@Configuration
public class ServerConfiguration {
    @Bean
    public RequestExecutorService executorService(
            AppConfig appConfig,
            List<ContextPropagator> contextPropagators) {

        String threadType = appConfig.getStringProperty(
            "server.thread-type", "virtual"
        );

        if (threadType.equals("virtual")) {
            return new VirtualRequestExecutorService(contextPropagators);
        }
        return new RequestExecutorPoolService(
            appConfig.getIntProperty("server.thread-pool-size", 100)
        );
    }
}
```

#### VirtualRequestExecutorService 구현

```java
public class VirtualRequestExecutorService implements RequestExecutorService {
    private final ExecutorService pool =
        Executors.newVirtualThreadPerTaskExecutor();
    private final List<ContextPropagator> propagators;

    @Override
    public void execute(Runnable task) {
        // 현재 컨텍스트를 캡처하고 가상 스레드로 전파
        final ContextSnapshot snapshot = new ContextSnapshot(propagators);
        pool.execute(snapshot.wrap(task));
    }
}
```

**주요 기능:**
- 작업당 새로운 가상 스레드 생성
- 최소한의 메모리 공간 (~1KB per thread)
- 자식 스레드로의 자동 컨텍스트 전파
- 수백만 개의 동시 연결에 적합

### 플랫폼 스레드 풀 모드

호환성을 위한 전통적인 고정 크기 스레드 풀:

```java
public class RequestExecutorPoolService implements RequestExecutorService {
    private final ExecutorService pool;

    public RequestExecutorPoolService(int threadPoolSize) {
        this.pool = Executors.newFixedThreadPool(threadPoolSize);
    }

    @Override
    public void execute(Runnable task) {
        pool.execute(task);
    }
}
```

**설정:**
```properties
# application.properties
server.thread-type=platform
server.thread-pool-size=200
```

**주요 기능:**
- 고정된 수의 플랫폼 스레드
- 예측 가능한 리소스 사용
- 모든 Java 버전과 호환
- 적당한 수준의 동시성 요구에 적합

## I/O 실행 모드

### 하이브리드 모드 (BIO with NIO Accept)

연결 수락을 위한 NIO와 요청 처리를 위한 BIO를 결합:

```java
public class BioHttpProtocolHandler implements AcceptableProtocolHandler {
    private final RequestDispatcher dispatcher;
    private final HttpRequestParser parser;
    private final RequestExecutorService requestExecutorService;

    @Override
    public void accept(SocketChannel channel, Selector selector,
                      ByteBuffer initialBuffer) throws Exception {
        // 1. NIO selector에서 분리
        detachFromSelector(channel, selector);

        // 2. 블로킹 모드로 전환
        channel.configureBlocking(true);
        Socket socket = channel.socket();

        // 3. 워커 스레드에 위임
        requestExecutorService.execute(() -> {
            try (InputStream in = socket.getInputStream();
                 BufferedWriter out = new BufferedWriter(
                     new OutputStreamWriter(socket.getOutputStream()))) {

                // 4. 완전한 요청 읽기 (블로킹)
                String rawRequest = HttpUtils.readRawRequest(initialBuffer, in);

                // 5. 파싱 및 디스패치
                HttpRequest<?> req = parser.parse(rawRequest);
                HttpResponse res = new HttpResponse();
                dispatcher.dispatch(req, res);

                // 6. 응답 쓰기 (블로킹)
                writeResponse(out, res.getResponseEntity());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
```

**흐름:**
1. NIO Selector가 연결 수락
2. 초기 바이트를 읽어 프로토콜 감지
3. 채널을 블로킹 모드로 전환
4. Selector에서 분리하고 워커 스레드에 위임
5. 워커 스레드에서 블로킹 I/O로 요청/응답 처리

**장점:**
- 간단한 프로그래밍 모델 (블로킹 I/O)
- 가상 스레드와 잘 작동
- 순수 NIO보다 낮은 복잡도
- 가상 스레드 executor와 함께 좋은 처리량

**설정:**
```properties
server.execution-mode=hybrid
server.thread-type=virtual
```

### 순수 NIO 모드

최대 확장성을 위한 완전한 논블로킹 I/O:

```java
public class NioHttpProtocolHandler implements AcceptableProtocolHandler {
    private final RequestDispatcher dispatcher;
    private final HttpRequestParser parser;
    private final RequestExecutorService requestExecutorService;

    @Override
    public void accept(SocketChannel channel, Selector selector,
                      ByteBuffer initialBuffer) throws Exception {
        // 1. 상태를 가진 연결 핸들러 생성
        HttpConnectionHandler handler = new HttpConnectionHandler(
            channel, selector, dispatcher, parser,
            requestExecutorService, initialBuffer
        );

        // 2. 핸들러를 attachment로 하여 READ 이벤트 등록
        channel.register(selector, SelectionKey.OP_READ, handler);

        // 3. 초기 읽기 트리거
        handler.read(channel.keyFor(selector));
    }
}
```

#### HttpConnectionHandler 상태 머신

```java
public class HttpConnectionHandler implements ReadableHandler, WritableHandler {
    private final SocketChannel channel;
    private final Selector selector;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(8192);
    private volatile ByteBuffer writeBuffer;
    private HttpConnectionStatus currentState = HttpConnectionStatus.READING;

    @Override
    public void read(SelectionKey key) throws Exception {
        if (currentState != HttpConnectionStatus.READING) return;

        // 1. 논블로킹 읽기
        int bytesRead = channel.read(readBuffer);
        if (bytesRead == -1) {
            closeConnection(key);
            return;
        }

        readBuffer.flip();

        // 2. 요청이 완료되었는지 확인
        if (HttpUtils.isRequestComplete(readBuffer)) {
            currentState = HttpConnectionStatus.PROCESSING;
            key.interestOps(0); // 이벤트 감지 중지

            // 3. 요청 추출
            byte[] requestBytes = new byte[readBuffer.remaining()];
            readBuffer.get(requestBytes);
            String rawRequest = new String(requestBytes, StandardCharsets.UTF_8);

            // 4. 워커 스레드에서 처리
            requestExecutorService.execute(() -> {
                try {
                    HttpRequest<?> req = parser.parse(rawRequest);
                    HttpResponse res = new HttpResponse();
                    dispatcher.dispatch(req, res);

                    // 5. 응답 준비 및 WRITING 상태로 전환
                    this.writeBuffer = HttpUtils.createResponseBuffer(
                        res.getResponseEntity()
                    );
                    this.currentState = HttpConnectionStatus.WRITING;

                    // 6. WRITE 이벤트 등록
                    key.interestOps(SelectionKey.OP_WRITE);
                    selector.wakeup();

                } catch (Exception e) {
                    closeConnection(key);
                    e.printStackTrace();
                }
            });

            readBuffer.clear();
        }
    }

    @Override
    public void write(SelectionKey key) throws IOException {
        if (currentState != HttpConnectionStatus.WRITING || writeBuffer == null)
            return;

        // 논블로킹 쓰기
        channel.write(writeBuffer);

        if (!writeBuffer.hasRemaining()) {
            // 모든 데이터 전송 완료
            currentState = HttpConnectionStatus.DONE;
            closeConnection(key);
        }
        // 데이터가 남아있으면, selector가 준비되었을 때 다시 write 트리거
    }
}
```

**상태 머신:**
```
READING → PROCESSING → WRITING → DONE
   ↑                                 ↓
   └─────────── (close/reset) ──────┘
```

**장점:**
- 최대 확장성 (단일 스레드가 수천 개의 연결 처리)
- 최소한의 스레드 컨텍스트 스위칭
- 효율적인 리소스 활용
- 높은 동시성 시나리오에 최적

**설정:**
```properties
server.execution-mode=nio
server.thread-type=platform
server.thread-pool-size=100
```

## NIO 이벤트 루프 아키텍처

### NioHybridServerStrategy

메인 이벤트 루프 구현:

```java
@Component
public class NioHybridServerStrategy implements ServerStrategy {
    private final ConnectionManager connectionManager;
    private volatile boolean running = true;
    private Selector selector;
    private ServerSocketChannel serverChannel;

    @Override
    public int start(int port) throws Exception {
        // 1. NIO selector 초기화
        selector = Selector.open();
        serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(port));
        serverChannel.configureBlocking(false);

        // 2. ACCEPT 이벤트 등록
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        // 3. 이벤트 루프 스레드 시작
        running = true;
        Thread t = new Thread(this::eventLoop, "sprout-nio-loop");
        t.setDaemon(false);
        t.start();

        return ((InetSocketAddress) serverChannel.getLocalAddress()).getPort();
    }

    private void eventLoop() {
        while (running) {
            selector.select(); // 이벤트가 준비될 때까지 블로킹

            for (Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                 it.hasNext();) {
                SelectionKey key = it.next();
                it.remove();

                if (!key.isValid()) {
                    cleanupConnection(key);
                    continue;
                }

                try {
                    // 새로운 연결 수락
                    if (key.isAcceptable()) {
                        connectionManager.acceptConnection(key, selector);
                    }

                    Object attachment = key.attachment();

                    // 읽기 가능 이벤트 처리
                    if (key.isReadable() && attachment instanceof ReadableHandler rh) {
                        rh.read(key);
                    }

                    // 쓰기 가능 이벤트 처리
                    if (key.isWritable() && attachment instanceof WritableHandler wh) {
                        wh.write(key);
                    }
                } catch (IOException ioe) {
                    cleanupConnection(key);
                } catch (Exception e) {
                    e.printStackTrace();
                    cleanupConnection(key);
                }
            }
        }
    }
}
```

**이벤트 루프 책임:**
- `ConnectionManager`를 통한 새로운 연결 수락
- `ReadableHandler`로 READ 이벤트 위임
- `WritableHandler`로 WRITE 이벤트 위임
- 오류 시 연결 정리

## 프로토콜 감지 및 라우팅

### 연결 수락 흐름

```java
@Component
public class DefaultConnectionManager implements ConnectionManager {
    private final List<ProtocolDetector> detectors;
    private final List<ProtocolHandler> handlers;

    @Override
    public void acceptConnection(SelectionKey selectionKey, Selector selector)
            throws Exception {
        // 1. 연결 수락
        ServerSocketChannel serverChannel =
            (ServerSocketChannel) selectionKey.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);

        // 2. 프로토콜 감지를 위한 초기 바이트 읽기
        ByteBuffer buffer = ByteBuffer.allocate(2048);
        int bytesRead = clientChannel.read(buffer);

        if (bytesRead <= 0) {
            clientChannel.close();
            return;
        }

        buffer.flip();

        // 3. 프로토콜 감지
        String detectedProtocol = "UNKNOWN";
        for (ProtocolDetector detector : detectors) {
            detectedProtocol = detector.detect(buffer);
            if (!"UNKNOWN".equals(detectedProtocol)) {
                break;
            }
        }

        if ("UNKNOWN".equals(detectedProtocol)) {
            clientChannel.close();
            return;
        }

        // 4. 적절한 핸들러로 라우팅
        for (ProtocolHandler handler : handlers) {
            if (handler.supports(detectedProtocol)) {
                if (handler instanceof AcceptableProtocolHandler) {
                    ((AcceptableProtocolHandler) handler)
                        .accept(clientChannel, selector, buffer);
                    return;
                }
            }
        }
    }
}
```

### HTTP 프로토콜 감지

```java
@Component
public class HttpProtocolDetector implements ProtocolDetector {
    private static final Set<String> HTTP_METHODS = Set.of(
        "GET ", "POST ", "PUT ", "DELETE ", "HEAD ",
        "OPTIONS ", "PATCH ", "TRACE "
    );

    @Override
    public String detect(ByteBuffer buffer) throws Exception {
        // 버퍼 위치 저장
        buffer.mark();

        // 처음 8바이트 읽기
        int readLimit = Math.min(buffer.remaining(), 8);
        byte[] headerBytes = new byte[readLimit];
        buffer.get(headerBytes);

        // 버퍼 위치 복원
        buffer.reset();

        String prefix = new String(headerBytes, StandardCharsets.UTF_8);

        // HTTP 메서드 확인
        if (HTTP_METHODS.stream().anyMatch(prefix::startsWith)) {
            return "HTTP/1.1";
        }

        return "UNKNOWN";
    }
}
```

**감지 프로세스:**
1. 연결에서 처음 바이트 읽기 (비파괴적)
2. HTTP 메서드 키워드 확인
3. 감지된 프로토콜 또는 "UNKNOWN" 반환
4. 후속 처리를 위해 버퍼 보존

## 요청 완료 감지

### HTTP 요청 파싱

```java
public final class HttpUtils {
    public static boolean isRequestComplete(ByteBuffer buffer) {
        // 1. 헤더 끝 찾기 (\r\n\r\n)
        byte[] arr = new byte[buffer.remaining()];
        buffer.get(arr);
        String content = new String(arr, StandardCharsets.UTF_8);
        int headerEnd = content.indexOf("\r\n\r\n");

        if (headerEnd < 0) {
            return false; // 헤더 불완전
        }

        String headers = content.substring(0, headerEnd);

        // 2. Content-Length 또는 Transfer-Encoding 확인
        int contentLength = parseContentLength(headers);
        boolean isChunked = isChunked(headers);

        int bodyStart = headerEnd + 4;
        int totalLength = content.length();

        if (isChunked) {
            // 청크 인코딩: 0\r\n\r\n 확인
            String body = content.substring(bodyStart);
            return isChunkedBodyComplete(body);
        } else if (contentLength >= 0) {
            // Content-Length: 본문 크기 확인
            int bodyReceived = totalLength - bodyStart;
            return bodyReceived >= contentLength;
        } else {
            // 본문 없음 (GET 요청)
            return true;
        }
    }
}
```

**완료 기준:**
- **헤더**: `\r\n\r\n` 포함해야 함
- **Content-Length**: 본문 바이트가 선언된 길이와 일치해야 함
- **Chunked**: 마지막 청크는 `0\r\n\r\n`이어야 함
- **본문 없음**: 헤더 이후 완료

## 모드 비교

### 하이브리드 모드 (BIO + 가상 스레드)

**최적 사용 시나리오:**
- 간단한 코드로 높은 처리량
- Java 21+ 프로젝트
- 적당한 요청 처리 시간을 가진 애플리케이션

**아키텍처:**
```
[NIO Selector] → Accept → [Detect Protocol] → [Switch to BIO]
                                                     ↓
                                            [Virtual Thread]
                                                     ↓
                                           [Blocking Read/Write]
```

**장점:**
- 간단하고 읽기 쉬운 코드
- 자동 백프레셔
- 가상 스레드와 잘 작동
- 디버깅 용이

**단점:**
- 순수 NIO보다 연결당 메모리 사용량 높음
- 스레드 전환 오버헤드

### 순수 NIO 모드

**최적 사용 시나리오:**
- 최대 확장성
- 낮은 지연시간 요구사항
- 리소스가 제한된 환경

**아키텍처:**
```
[NIO Selector] → Accept → [Detect Protocol] → [Register READ]
       ↑                                              ↓
       └──────────[Write Complete]───[Process in Pool Thread]
```

**장점:**
- 단일 스레드가 수천 개의 연결 처리
- 최소 메모리 공간
- 낮은 지연시간

**단점:**
- 복잡한 상태 머신
- 디버깅 어려움
- 세심한 버퍼 관리 필요

## 설정 가이드

### 권장 설정

#### 높은 처리량 API 서버 (Java 21+)
```properties
server.execution-mode=hybrid
server.thread-type=virtual
```

#### 최대 확장성 (연결 집약적)
```properties
server.execution-mode=nio
server.thread-type=platform
server.thread-pool-size=200
```

#### 레거시 호환성 (Java 11/17)
```properties
server.execution-mode=hybrid
server.thread-type=platform
server.thread-pool-size=500
```

## 모범 사례

### 1. 올바른 모드 선택

```java
// 대부분의 애플리케이션 (Java 21+)
server.execution-mode=hybrid
server.thread-type=virtual

// 극한의 확장성 필요 시
server.execution-mode=nio
server.thread-type=platform
```

### 2. 컨텍스트 전파

가상 스레드 executor는 자동으로 컨텍스트를 전파합니다:

```java
public class VirtualRequestExecutorService {
    @Override
    public void execute(Runnable task) {
        // 태스크 제출 전에 컨텍스트 캡처
        final ContextSnapshot snapshot = new ContextSnapshot(propagators);
        pool.execute(snapshot.wrap(task));
    }
}
```

### 3. 우아한 종료

```java
@Component
public class ServerShutdownHook {
    private final HttpServer server;

    @PreDestroy
    public void shutdown() throws Exception {
        server.stop();
    }
}
```

### 4. 스레드 사용량 모니터링

```java
// 플랫폼 스레드: 스레드 풀 포화도 모니터링
// 가상 스레드: 메모리 및 CPU 사용량 모니터링
```

## 성능 특성

### 가상 스레드 모드
- **확장성**: 높은 연결 수에 탁월

### 플랫폼 스레드 풀 모드
- **확장성**: 스레드 풀 크기에 의해 제한됨

### NIO vs 하이브리드
- **NIO**: 낮은 메모리, 높은 복잡도
- **하이브리드**: 가상 스레드와 함께 높은 처리량, 간단한 코드
- **하이브리드 + 가상**: 현대적인 애플리케이션에 최적의 균형

## 확장 포인트

### 커스텀 프로토콜 핸들러

```java
@Component
public class CustomProtocolHandler implements AcceptableProtocolHandler {
    @Override
    public void accept(SocketChannel channel, Selector selector,
                      ByteBuffer buffer) throws Exception {
        // 커스텀 프로토콜 처리 로직
    }

    @Override
    public boolean supports(String protocol) {
        return "CUSTOM/1.0".equals(protocol);
    }
}
```

### 커스텀 프로토콜 감지기

```java
@Component
public class CustomProtocolDetector implements ProtocolDetector {
    @Override
    public String detect(ByteBuffer buffer) throws Exception {
        // 버퍼를 검사하고 프로토콜 이름 반환
        return "CUSTOM/1.0";
    }
}
```
