package sprout.server.builtins;

import sprout.mvc.dispatcher.RequestDispatcher;
import sprout.mvc.http.parser.HttpRequestParser;
import sprout.server.AcceptableProtocolHandler;
import sprout.server.RequestExecutorService;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class NioHttpProtocolHandler implements AcceptableProtocolHandler {
    private final RequestDispatcher dispatcher;
    private final HttpRequestParser parser;
    private final RequestExecutorService requestExecutorService;


    public NioHttpProtocolHandler(RequestDispatcher dispatcher, HttpRequestParser parser, RequestExecutorService requestExecutorService) {
        this.dispatcher = dispatcher;
        this.parser = parser;
        this.requestExecutorService = requestExecutorService;
    }

    @Override
    public void accept(SocketChannel channel, Selector selector, ByteBuffer byteBuffer) throws Exception {
        System.out.println( "Accepted connection from " + channel.socket());
        HttpConnectionHandler handler = new HttpConnectionHandler(channel, selector, dispatcher, parser, requestExecutorService, byteBuffer);
        channel.register(selector, SelectionKey.OP_READ, handler);
        handler.read(channel.keyFor(selector));
    }

    @Override
    public boolean supports(String protocol) {
        return "HTTP/1.1".equals(protocol);
    }
}
