package sprout.server.builtins;

import sprout.mvc.dispatcher.RequestDispatcher;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.HttpResponse;
import sprout.mvc.http.parser.HttpRequestParser;
import sprout.server.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class HttpConnectionHandler implements ReadableHandler, WritableHandler {

    private final SocketChannel channel;
    private final Selector selector;
    private final RequestDispatcher dispatcher;
    private final HttpRequestParser parser;
    private final RequestExecutorService requestExecutorService;

    private final ByteBuffer readBuffer = ByteBuffer.allocate(8192);
    private volatile ByteBuffer writeBuffer;
    private HttpConnectionStatus currentState = HttpConnectionStatus.READING;

    public HttpConnectionHandler(SocketChannel channel, Selector selector, RequestDispatcher dispatcher, HttpRequestParser parser, RequestExecutorService requestExecutorService) {
        this.channel = channel;
        this.selector = selector;
        this.dispatcher = dispatcher;
        this.parser = parser;
        this.requestExecutorService = requestExecutorService;
    }

    @Override
    public void read(SelectionKey key) throws Exception {
        if (currentState == HttpConnectionStatus.READING) return;

        int bytesRead = channel.read(readBuffer);
        if (bytesRead == -1) {
            closeConnection(key);
            return;
        }

        if (HttpUtils.isRequestComplete(readBuffer)) {
            // 3. 완전한 요청이 왔다면, 처리 상태로 변경
            this.currentState = HttpConnectionStatus.PROCESSING;
            key.interestOps(0); // 이벤트 감지 일단 중지

            // readBuffer에서 요청 전문(raw request)을 추출
            readBuffer.flip();
            byte[] requestBytes = new byte[readBuffer.remaining()];
            readBuffer.get(requestBytes);
            String rawRequest = new String(requestBytes, StandardCharsets.UTF_8);

            // 4. 비즈니스 로직은 스레드 풀에 위임 (기존과 동일)
            requestExecutorService.execute(() -> {
                try {
                    HttpRequest<?> req = parser.parse(rawRequest);
                    HttpResponse res = new HttpResponse();
                    dispatcher.dispatch(req, res);

                    // 5. 응답 준비 및 쓰기 상태 전환
                    this.writeBuffer = HttpUtils.createResponseBuffer(res.getResponseEntity());
                    this.currentState = HttpConnectionStatus.WRITING;

                    // 6. Selector에 쓰기 이벤트 감지 요청
                    key.interestOps(SelectionKey.OP_WRITE);
                    selector.wakeup();

                } catch (Exception e) {
                    closeConnection(key);
                    e.printStackTrace();
                }
            });

            // 버퍼 초기화 (다음 요청을 위해)
            readBuffer.clear();
        }

    }

    private void closeConnection(SelectionKey key) {
        try {
            key.cancel();
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void write(SelectionKey key) throws IOException {
        if (currentState != HttpConnectionStatus.WRITING || writeBuffer == null) return;

        channel.write(writeBuffer);

        if (!writeBuffer.hasRemaining()) {
            // 버퍼의 모든 데이터를 전송 완료
            this.currentState = HttpConnectionStatus.DONE;
            closeConnection(key);
        }
        // 버퍼에 데이터가 남아있다면 아무것도 하지 않음
        // 채널이 다시 쓸 준비가 되면 셀렉터가 알려줄 것
    }
}
