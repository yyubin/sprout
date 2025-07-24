package sprout.server.websocket.framehandler;

import sprout.server.websocket.WebSocketFrame;
import sprout.server.websocket.message.DefaultMessagePayload;
import sprout.server.websocket.message.MessagePayload;

import java.io.ByteArrayOutputStream;

public class FrameProcessingContext {
    private WebSocketFrame currentFrame; // 현재 처리 중인 프레임
    private final StringBuilder textBuffer;
    private final ByteArrayOutputStream binaryBuffer;
    private int fragmentedOpcode;

    public FrameProcessingContext() {
        this.textBuffer = new StringBuilder();
        this.binaryBuffer = new ByteArrayOutputStream();
        this.fragmentedOpcode = -1;
    }

    public void setCurrentFrame(WebSocketFrame frame) {
        this.currentFrame = frame;
    }

    public void setFragmentedOpcode(int fragmentedOpcode) {
        this.fragmentedOpcode = fragmentedOpcode;
    }

    public WebSocketFrame getFrame() { return currentFrame; }
    public StringBuilder getTextBuffer() { return textBuffer; }
    public ByteArrayOutputStream getBinaryBuffer() { return binaryBuffer; }
    public int getFragmentedOpcode() { return fragmentedOpcode; }

    public boolean isFragmented() {
        return fragmentedOpcode != -1;
    }

    public void startFragmentedMessage(int opcode) {
        this.fragmentedOpcode = opcode;
    }

    public void reset() {
        this.textBuffer.setLength(0); // new StringBuilder() 대신 내용만 비움
        this.binaryBuffer.reset();    // new ByteArrayOutputStream() 대신 내용만 비움
        this.fragmentedOpcode = -1;
        this.currentFrame = null;
    }

    public MessagePayload createPayload() {
        String text = textBuffer.length() > 0 ? textBuffer.toString() : null;
        byte[] binary = binaryBuffer.size() > 0 ? binaryBuffer.toByteArray() : null;
        return new DefaultMessagePayload(text, binary);
    }
}
