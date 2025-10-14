package sprout.server;

import sprout.mvc.http.ResponseEntity;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class HttpUtils {
    private HttpUtils() {}

    public static boolean isRequestComplete(ByteBuffer buffer) {
        // 버퍼가 비어있거나 읽을 데이터가 없으면 false
        if (buffer == null || !buffer.hasRemaining()) {
            return false;
        }

        // 현재 버퍼 위치 저장 및 작업 후 복원
        int originalPosition = buffer.position();
        try {
            // 헤더 끝(\r\n\r\n) 찾기
            int headerEnd = -1;
            byte[] arr = new byte[buffer.remaining()];
            buffer.get(arr);
            String content = new String(arr, StandardCharsets.UTF_8);
            headerEnd = content.indexOf("\r\n\r\n");

            if (headerEnd < 0) {
                return false; // 헤더 끝이 없으면 요청 미완성
            }

            // 헤더 부분 추출
            String headers = content.substring(0, headerEnd);

            // Content-Length 확인
            int contentLength = parseContentLength(headers);
            boolean isChunked = isChunked(headers);

            // 헤더 이후 바디 데이터 시작 위치
            int bodyStart = headerEnd + 4;
            int totalLength = content.length();

            if (isChunked) {
                // 청크드 인코딩 처리
                String body = content.substring(bodyStart);
                return isChunkedBodyComplete(body);
            } else if (contentLength >= 0) {
                // Content-Length가 명시된 경우
                int bodyReceived = totalLength - bodyStart;
                return bodyReceived >= contentLength;
            } else {
                // 바디가 없는 경우 (예: GET 요청)
                return true;
            }
        } finally {
            // 버퍼 위치 복원
            buffer.position(originalPosition);
        }
    }

    // 청크드 바디가 완전한지 확인하는 헬퍼 메서드
    private static boolean isChunkedBodyComplete(String body) {
        int pos = 0;
        while (pos < body.length()) {
            int lineEnd = body.indexOf("\r\n", pos);
            if (lineEnd < 0) {
                return false; // 청크 크기 라인이 없음
            }
            String lenLine = body.substring(pos, lineEnd);
            int len;
            try {
                len = Integer.parseInt(lenLine.trim(), 16);
            } catch (NumberFormatException e) {
                return false; // 잘못된 청크 크기
            }

            if (len == 0) {
                // 마지막 청크 (0\r\n\r\n)
                return body.substring(lineEnd).startsWith("\r\n\r\n");
            }

            // 청크 데이터 확인
            pos = lineEnd + 2; // CRLF 건너뛰기
            if (pos + len + 2 > body.length()) {
                return false; // 청크 데이터가 충분히 수신되지 않음
            }
            pos += len + 2; // 청크 데이터 + CRLF
        }
        return false; // 청크 끝에 도달하지 못함
    }

    public static String readRawRequest(ByteBuffer initial, InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();

        // 1) initial buffer
        if (initial != null && initial.hasRemaining()) {
            byte[] arr = new byte[initial.remaining()];
            initial.get(arr);
            sb.append(new String(arr, StandardCharsets.UTF_8));
        }

        // 2) 헤더 끝까지 읽기
        BufferedInputStream bin = new BufferedInputStream(in);
        while (!sb.toString().contains("\r\n\r\n")) {
            int ch = bin.read();
            if (ch == -1) break; // 연결 끊김
            sb.append((char) ch);
        }

        // 파싱해서 Content-Length or chunked 확인
        String headerPart = sb.toString();
        int headerEnd = headerPart.indexOf("\r\n\r\n");
        if (headerEnd < 0) return headerPart; // 잘못된 요청

        String headers = headerPart.substring(0, headerEnd);
        String bodyStart = headerPart.substring(headerEnd + 4);

        int contentLength = parseContentLength(headers); // 없으면 -1
        boolean chunked = isChunked(headers);

        if (chunked) {
            // TODO: chunked 디코딩
            bodyStart += readChunkedBody(bin);
        } else if (contentLength > -1) {
            int alreadyRead = bodyStart.getBytes(StandardCharsets.UTF_8).length;
            int remaining = contentLength - alreadyRead;
            if (remaining > 0) {
                byte[] bodyBytes = bin.readNBytes(remaining);
                bodyStart += new String(bodyBytes, StandardCharsets.UTF_8);
            }
        }

        return headers + "\r\n\r\n" + bodyStart;
    }

    private static int parseContentLength(String headers) {
        for (String line : headers.split("\r\n")) {
            if (line.toLowerCase().startsWith("content-length:")) {
                return Integer.parseInt(line.split(":")[1].trim());
            }
        }
        return -1;
    }

    private static boolean isChunked(String headers) {
        for (String line : headers.split("\r\n")) {
            if (line.toLowerCase().startsWith("transfer-encoding:")
                    && line.toLowerCase().contains("chunked")) {
                return true;
            }
        }
        return false;
    }

    private static String readChunkedBody(InputStream in) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder body = new StringBuilder();
        while (true) {
            String lenLine = r.readLine();
            if (lenLine == null) break;
            int len = Integer.parseInt(lenLine.trim(), 16);
            if (len == 0) {
                // trailing headers consume up to empty line
                while (!"".equals(r.readLine())) {}
                break;
            }
            char[] buf = new char[len];
            int read = r.read(buf);
            body.append(buf, 0, read);
            r.readLine(); // consume CRLF
        }
        return body.toString();
    }

    public static ByteBuffer createResponseBuffer(ResponseEntity<?> res) {
        if (res == null) return null;

        // Body를 바이트로 변환 (UTF-8)
        byte[] bodyBytes = res.getBody() != null
            ? res.getBody().toString().getBytes(StandardCharsets.UTF_8)
            : new byte[0];

        // HTTP 헤더 작성
        StringBuilder header = new StringBuilder();
        header.append("HTTP/1.1 ")
              .append(res.getStatusCode().getCode())
              .append(" ")
              .append(res.getStatusCode().getMessage())
              .append("\r\n");

        // Content-Type
        header.append("Content-Type: ")
              .append(res.getContentType())
              .append("\r\n");

        // Content-Length (바이트 단위로 정확히)
        header.append("Content-Length: ")
              .append(bodyBytes.length)
              .append("\r\n");

        // Connection 헤더: keep-alive 활성화 (HTTP/1.1 기본)
        header.append("Connection: keep-alive\r\n");
        header.append("Keep-Alive: timeout=5, max=1000\r\n");

        // Custom headers
        if (res.getHeaders() != null) {
            for (Map.Entry<String, String> entry : res.getHeaders().entrySet()) {
                header.append(entry.getKey())
                      .append(": ")
                      .append(entry.getValue())
                      .append("\r\n");
            }
        }

        // 헤더 끝
        header.append("\r\n");

        // 헤더 바이트
        byte[] headerBytes = header.toString().getBytes(StandardCharsets.UTF_8);

        // 전체 응답 버퍼 생성 (헤더 + 바디)
        ByteBuffer buffer = ByteBuffer.allocate(headerBytes.length + bodyBytes.length);
        buffer.put(headerBytes);
        buffer.put(bodyBytes);
        buffer.flip();

        return buffer;
    }
}
