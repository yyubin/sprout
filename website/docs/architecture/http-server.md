# 🌐 HTTP Server

The HTTP Server is Sprout's embedded server implementation that replicates Tomcat's core functionality. It provides a flexible, configurable server infrastructure supporting both blocking I/O (BIO) and non-blocking I/O (NIO), with options for platform threads or virtual threads.

## Overview

Sprout's HTTP Server provides:
- **NIO-based Event Loop**: High-performance non-blocking I/O using Java NIO Selector
- **Hybrid BIO/NIO Mode**: Flexible I/O strategy selection per protocol handler
- **Virtual Thread Support**: Modern concurrency with Java 21's virtual threads
- **Platform Thread Pool**: Traditional thread pool for compatibility
- **Protocol Detection**: Automatic HTTP/WebSocket protocol identification
- **Pluggable Handler Architecture**: Extensible protocol handler system

## Server Architecture

### Core Components

The HTTP Server consists of the following key components:

- `HttpServer`: Main server facade implementing lifecycle management
- `ServerStrategy`: Pluggable server strategy interface (NIO event loop)
- `ConnectionManager`: Connection acceptance and protocol routing
- `ProtocolDetector`: Protocol identification from initial bytes
- `ProtocolHandler`: Protocol-specific request handling
- `RequestExecutorService`: Thread management abstraction

### Server Initialization Process

```java
public class SproutApplication {
    public static void run(Class<?> primarySource) throws Exception {
        // 1. Create application context
        ApplicationContext ctx = new SproutApplicationContext(packages);
        ctx.refresh();

        // 2. Get HttpServer bean
        HttpServer server = ctx.getBean(HttpServer.class);

        // 3. Start server
        int port = server.start(8080);
        System.out.println("Server started on port " + port);
    }
}
```

## Thread Execution Modes

### Virtual Thread Mode (Default)

Virtual threads provide lightweight concurrency for high-throughput applications:

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

#### VirtualRequestExecutorService Implementation

```java
public class VirtualRequestExecutorService implements RequestExecutorService {
    private final ExecutorService pool =
        Executors.newVirtualThreadPerTaskExecutor();
    private final List<ContextPropagator> propagators;

    @Override
    public void execute(Runnable task) {
        // Capture current context and propagate to virtual thread
        final ContextSnapshot snapshot = new ContextSnapshot(propagators);
        pool.execute(snapshot.wrap(task));
    }
}
```

**Key Features:**
- Creates a new virtual thread per task
- Minimal memory footprint (~1KB per thread)
- Automatic context propagation to child threads
- Suitable for millions of concurrent connections

### Platform Thread Pool Mode

Traditional fixed-size thread pool for compatibility:

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

**Configuration:**
```properties
# application.properties
server.thread-type=platform
server.thread-pool-size=200
```

**Key Features:**
- Fixed number of platform threads
- Predictable resource usage
- Compatible with all Java versions
- Suitable for moderate concurrency needs

## I/O Execution Modes

### Hybrid Mode (BIO with NIO Accept)

Combines NIO for connection acceptance with BIO for request processing:

```java
public class BioHttpProtocolHandler implements AcceptableProtocolHandler {
    private final RequestDispatcher dispatcher;
    private final HttpRequestParser parser;
    private final RequestExecutorService requestExecutorService;

    @Override
    public void accept(SocketChannel channel, Selector selector,
                      ByteBuffer initialBuffer) throws Exception {
        // 1. Detach from NIO selector
        detachFromSelector(channel, selector);

        // 2. Switch to blocking mode
        channel.configureBlocking(true);
        Socket socket = channel.socket();

        // 3. Delegate to worker thread
        requestExecutorService.execute(() -> {
            try (InputStream in = socket.getInputStream();
                 BufferedWriter out = new BufferedWriter(
                     new OutputStreamWriter(socket.getOutputStream()))) {

                // 4. Read complete request (blocking)
                String rawRequest = HttpUtils.readRawRequest(initialBuffer, in);

                // 5. Parse and dispatch
                HttpRequest<?> req = parser.parse(rawRequest);
                HttpResponse res = new HttpResponse();
                dispatcher.dispatch(req, res);

                // 6. Write response (blocking)
                writeResponse(out, res.getResponseEntity());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
```

**Flow:**
1. NIO Selector accepts connection
2. Read initial bytes to detect protocol
3. Switch channel to blocking mode
4. Detach from selector and delegate to worker thread
5. Blocking I/O for request/response in worker thread

**Advantages:**
- Simple programming model (blocking I/O)
- Works well with virtual threads
- Lower complexity than pure NIO
- Good throughput with virtual thread executor

**Configuration:**
```properties
server.execution-mode=hybrid
server.thread-type=virtual
```

### Pure NIO Mode

Fully non-blocking I/O for maximum scalability:

```java
public class NioHttpProtocolHandler implements AcceptableProtocolHandler {
    private final RequestDispatcher dispatcher;
    private final HttpRequestParser parser;
    private final RequestExecutorService requestExecutorService;

    @Override
    public void accept(SocketChannel channel, Selector selector,
                      ByteBuffer initialBuffer) throws Exception {
        // 1. Create stateful connection handler
        HttpConnectionHandler handler = new HttpConnectionHandler(
            channel, selector, dispatcher, parser,
            requestExecutorService, initialBuffer
        );

        // 2. Register for READ events with handler as attachment
        channel.register(selector, SelectionKey.OP_READ, handler);

        // 3. Trigger initial read
        handler.read(channel.keyFor(selector));
    }
}
```

#### HttpConnectionHandler State Machine

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

        // 1. Non-blocking read
        int bytesRead = channel.read(readBuffer);
        if (bytesRead == -1) {
            closeConnection(key);
            return;
        }

        readBuffer.flip();

        // 2. Check if request is complete
        if (HttpUtils.isRequestComplete(readBuffer)) {
            currentState = HttpConnectionStatus.PROCESSING;
            key.interestOps(0); // Stop event detection

            // 3. Extract request
            byte[] requestBytes = new byte[readBuffer.remaining()];
            readBuffer.get(requestBytes);
            String rawRequest = new String(requestBytes, StandardCharsets.UTF_8);

            // 4. Process in worker thread
            requestExecutorService.execute(() -> {
                try {
                    HttpRequest<?> req = parser.parse(rawRequest);
                    HttpResponse res = new HttpResponse();
                    dispatcher.dispatch(req, res);

                    // 5. Prepare response and switch to WRITING state
                    this.writeBuffer = HttpUtils.createResponseBuffer(
                        res.getResponseEntity()
                    );
                    this.currentState = HttpConnectionStatus.WRITING;

                    // 6. Register for WRITE events
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

        // Non-blocking write
        channel.write(writeBuffer);

        if (!writeBuffer.hasRemaining()) {
            // All data sent
            currentState = HttpConnectionStatus.DONE;
            closeConnection(key);
        }
        // If data remains, selector will trigger write again when ready
    }
}
```

**State Machine:**
```
READING → PROCESSING → WRITING → DONE
   ↑                                 ↓
   └─────────── (close/reset) ──────┘
```

**Advantages:**
- Maximum scalability (single thread handles thousands of connections)
- Minimal thread context switching
- Efficient resource utilization
- Best for high-concurrency scenarios

**Configuration:**
```properties
server.execution-mode=nio
server.thread-type=platform
server.thread-pool-size=100
```

## NIO Event Loop Architecture

### NioHybridServerStrategy

The main event loop implementation:

```java
@Component
public class NioHybridServerStrategy implements ServerStrategy {
    private final ConnectionManager connectionManager;
    private volatile boolean running = true;
    private Selector selector;
    private ServerSocketChannel serverChannel;

    @Override
    public int start(int port) throws Exception {
        // 1. Initialize NIO selector
        selector = Selector.open();
        serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(port));
        serverChannel.configureBlocking(false);

        // 2. Register ACCEPT event
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        // 3. Start event loop thread
        running = true;
        Thread t = new Thread(this::eventLoop, "sprout-nio-loop");
        t.setDaemon(false);
        t.start();

        return ((InetSocketAddress) serverChannel.getLocalAddress()).getPort();
    }

    private void eventLoop() {
        while (running) {
            selector.select(); // Block until events are ready

            for (Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                 it.hasNext();) {
                SelectionKey key = it.next();
                it.remove();

                if (!key.isValid()) {
                    cleanupConnection(key);
                    continue;
                }

                try {
                    // Accept new connections
                    if (key.isAcceptable()) {
                        connectionManager.acceptConnection(key, selector);
                    }

                    Object attachment = key.attachment();

                    // Handle readable events
                    if (key.isReadable() && attachment instanceof ReadableHandler rh) {
                        rh.read(key);
                    }

                    // Handle writable events
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

**Event Loop Responsibilities:**
- Accept new connections via `ConnectionManager`
- Delegate READ events to `ReadableHandler`
- Delegate WRITE events to `WritableHandler`
- Connection cleanup on errors

## Protocol Detection and Routing

### Connection Acceptance Flow

```java
@Component
public class DefaultConnectionManager implements ConnectionManager {
    private final List<ProtocolDetector> detectors;
    private final List<ProtocolHandler> handlers;

    @Override
    public void acceptConnection(SelectionKey selectionKey, Selector selector)
            throws Exception {
        // 1. Accept connection
        ServerSocketChannel serverChannel =
            (ServerSocketChannel) selectionKey.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);

        // 2. Read initial bytes for protocol detection
        ByteBuffer buffer = ByteBuffer.allocate(2048);
        int bytesRead = clientChannel.read(buffer);

        if (bytesRead <= 0) {
            clientChannel.close();
            return;
        }

        buffer.flip();

        // 3. Detect protocol
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

        // 4. Route to appropriate handler
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

### HTTP Protocol Detection

```java
@Component
public class HttpProtocolDetector implements ProtocolDetector {
    private static final Set<String> HTTP_METHODS = Set.of(
        "GET ", "POST ", "PUT ", "DELETE ", "HEAD ",
        "OPTIONS ", "PATCH ", "TRACE "
    );

    @Override
    public String detect(ByteBuffer buffer) throws Exception {
        // Save buffer position
        buffer.mark();

        // Read first 8 bytes
        int readLimit = Math.min(buffer.remaining(), 8);
        byte[] headerBytes = new byte[readLimit];
        buffer.get(headerBytes);

        // Restore buffer position
        buffer.reset();

        String prefix = new String(headerBytes, StandardCharsets.UTF_8);

        // Check for HTTP method
        if (HTTP_METHODS.stream().anyMatch(prefix::startsWith)) {
            return "HTTP/1.1";
        }

        return "UNKNOWN";
    }
}
```

**Detection Process:**
1. Read first bytes from connection (non-destructive)
2. Check for HTTP method keywords
3. Return detected protocol or "UNKNOWN"
4. Preserve buffer for subsequent processing

## Request Completion Detection

### HTTP Request Parsing

```java
public final class HttpUtils {
    public static boolean isRequestComplete(ByteBuffer buffer) {
        // 1. Find header end (\r\n\r\n)
        byte[] arr = new byte[buffer.remaining()];
        buffer.get(arr);
        String content = new String(arr, StandardCharsets.UTF_8);
        int headerEnd = content.indexOf("\r\n\r\n");

        if (headerEnd < 0) {
            return false; // Headers incomplete
        }

        String headers = content.substring(0, headerEnd);

        // 2. Check Content-Length or Transfer-Encoding
        int contentLength = parseContentLength(headers);
        boolean isChunked = isChunked(headers);

        int bodyStart = headerEnd + 4;
        int totalLength = content.length();

        if (isChunked) {
            // Chunked encoding: check for 0\r\n\r\n
            String body = content.substring(bodyStart);
            return isChunkedBodyComplete(body);
        } else if (contentLength >= 0) {
            // Content-Length: verify body size
            int bodyReceived = totalLength - bodyStart;
            return bodyReceived >= contentLength;
        } else {
            // No body (GET request)
            return true;
        }
    }
}
```

**Completion Criteria:**
- **Headers**: Must contain `\r\n\r\n`
- **Content-Length**: Body bytes must match declared length
- **Chunked**: Last chunk must be `0\r\n\r\n`
- **No Body**: Complete after headers

## Mode Comparison

### Hybrid Mode (BIO + Virtual Threads)

**Best For:**
- High throughput with simple code
- Java 21+ projects
- Applications with moderate request processing time

**Architecture:**
```
[NIO Selector] → Accept → [Detect Protocol] → [Switch to BIO]
                                                     ↓
                                            [Virtual Thread]
                                                     ↓
                                           [Blocking Read/Write]
```

**Pros:**
- Simple, readable code
- Automatic backpressure
- Works well with virtual threads
- Easy to debug

**Cons:**
- Higher memory per connection than pure NIO
- Thread switching overhead

### Pure NIO Mode

**Best For:**
- Maximum scalability
- Low-latency requirements
- Resource-constrained environments

**Architecture:**
```
[NIO Selector] → Accept → [Detect Protocol] → [Register READ]
       ↑                                              ↓
       └──────────[Write Complete]───[Process in Pool Thread]
```

**Pros:**
- Single thread handles thousands of connections
- Minimal memory footprint
- Low latency

**Cons:**
- Complex state machine
- Harder to debug
- Requires careful buffer management

## Configuration Guide

### Recommended Configurations

#### High-Throughput API Server (Java 21+)
```properties
server.execution-mode=hybrid
server.thread-type=virtual
```

#### Maximum Scalability (Connection-heavy)
```properties
server.execution-mode=nio
server.thread-type=platform
server.thread-pool-size=200
```

#### Legacy Compatibility (Java 11/17)
```properties
server.execution-mode=hybrid
server.thread-type=platform
server.thread-pool-size=500
```

## Best Practices

### 1. Choose the Right Mode

```java
// For most applications (Java 21+)
server.execution-mode=hybrid
server.thread-type=virtual

// For extreme scalability needs
server.execution-mode=nio
server.thread-type=platform
```

### 2. Context Propagation

Virtual thread executor automatically propagates context:

```java
public class VirtualRequestExecutorService {
    @Override
    public void execute(Runnable task) {
        // Context is captured before task submission
        final ContextSnapshot snapshot = new ContextSnapshot(propagators);
        pool.execute(snapshot.wrap(task));
    }
}
```

### 3. Graceful Shutdown

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

### 4. Monitor Thread Usage

```java
// Platform threads: monitor thread pool saturation
// Virtual threads: monitor memory and CPU usage
```

## Performance Characteristics

### Virtual Thread Mode
- **Scalability**: Excellent for high connection count

### Platform Thread Pool Mode
- **Scalability**: Limited by thread pool size

### NIO vs Hybrid
- **NIO**: Lower memory, higher complexity
- **Hybrid**: Higher throughput with virtual threads, simpler code
- **Hybrid + Virtual**: Best balance for modern applications

## Extension Points

### Custom Protocol Handler

```java
@Component
public class CustomProtocolHandler implements AcceptableProtocolHandler {
    @Override
    public void accept(SocketChannel channel, Selector selector,
                      ByteBuffer buffer) throws Exception {
        // Custom protocol handling logic
    }

    @Override
    public boolean supports(String protocol) {
        return "CUSTOM/1.0".equals(protocol);
    }
}
```

### Custom Protocol Detector

```java
@Component
public class CustomProtocolDetector implements ProtocolDetector {
    @Override
    public String detect(ByteBuffer buffer) throws Exception {
        // Inspect buffer and return protocol name
        return "CUSTOM/1.0";
    }
}
```
> Refer to the [Performance Test and Optimization] (./performance-optimization.md ) document for performance optimization process and benchmark results.