package sprout.server.websocket.endpoint;

import sprout.server.exception.EncodeException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;

public interface Encoder {
    default void init(EndpointConfig endpointConfig) {
    }

    default void destroy() {
    }

    interface Text<T> extends Encoder {

        String encode(T object) throws EncodeException;
    }

    interface TextStream<T> extends Encoder {

        void encode(T object, Writer writer) throws EncodeException, IOException;
    }

    interface Binary<T> extends Encoder {

        ByteBuffer encode(T object) throws EncodeException;
    }

    interface BinaryStream<T> extends Encoder {

        void encode(T object, OutputStream os) throws EncodeException, IOException;
    }
}
