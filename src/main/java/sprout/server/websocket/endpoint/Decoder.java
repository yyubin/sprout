package sprout.server.websocket.endpoint;

import sprout.server.exception.DecodeException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;

public interface Decoder {

    default void init(EndpointConfig endpointConfig) {
    }

    default void destroy() {
    }

    interface Binary<T> extends Decoder {

        T decode(ByteBuffer bytes) throws DecodeException;

        boolean willDecode(ByteBuffer bytes);
    }

    interface BinaryStream<T> extends Decoder {

        T decode(InputStream is) throws DecodeException, IOException;
    }

    interface Text<T> extends Decoder {

        T decode(String s) throws DecodeException;

        boolean willDecode(String s);
    }

    interface TextStream<T> extends Decoder {

        T decode(Reader reader) throws DecodeException, IOException;
    }
}
