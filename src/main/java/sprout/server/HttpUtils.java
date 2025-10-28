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
        // BufferedInputStream을 한 번만 생성하여 재사용 (데이터 손실 방지)
        BufferedInputStream bin = new BufferedInputStream(in);

        // Phase 1: 헤더만 먼저 읽기 (메서드 분리)
        String headerPart = readHeadersFromStream(initial, bin);

        // Phase 2: 조기 리턴 - 헤더가 불완전한 경우
        int headerEnd = headerPart.indexOf("\r\n\r\n");
        if (headerEnd < 0) {
            return headerPart; // 잘못된 요청
        }

        String headers = headerPart.substring(0, headerEnd);
        String bodyStart = headerPart.substring(headerEnd + 4);

        // Phase 2: 조기 리턴 - Content-Length 케이스 (대부분의 HTTP 요청, 80%+)
        int contentLength = parseContentLength(headers);
        if (contentLength > 0) {
            String body = readBodyWithContentLength(bin, contentLength, bodyStart);
            return headers + "\r\n\r\n" + body;
        }

        // Content-Length: 0인 경우 (바디 없는 POST 등)
        if (contentLength == 0) {
            return headers + "\r\n\r\n" + bodyStart;
        }

        // Phase 2: 조기 리턴 - Chunked 케이스 (10% 미만)
        if (isChunked(headers)) {
            String chunkedBody = readChunkedBody(bin);
            return headers + "\r\n\r\n" + bodyStart + chunkedBody;
        }

        // Phase 2: 조기 리턴 - 바디가 없는 케이스 (GET 등)
        return headers + "\r\n\r\n" + bodyStart;
    }

    private static String readHeadersFromStream(ByteBuffer initial, BufferedInputStream bin) throws IOException {
        StringBuilder sb = new StringBuilder();

        // 1) initial buffer 처리
        if (initial != null && initial.hasRemaining()) {
            byte[] arr = new byte[initial.remaining()];
            initial.get(arr);
            sb.append(new String(arr, StandardCharsets.UTF_8));
        }

        // 2) 헤더 끝(\r\n\r\n)까지 읽기
        while (!sb.toString().contains("\r\n\r\n")) {
            int ch = bin.read();
            if (ch == -1) break; // 연결 끊김
            sb.append((char) ch);
        }

        return sb.toString();
    }

    private static String readBodyWithContentLength(BufferedInputStream bin, int contentLength, String bodyStart) throws IOException {
        int alreadyRead = bodyStart.getBytes(StandardCharsets.UTF_8).length;
        int remaining = contentLength - alreadyRead;

        if (remaining <= 0) {
            return bodyStart;
        }

        byte[] bodyBytes = bin.readNBytes(remaining);
        return bodyStart + new String(bodyBytes, StandardCharsets.UTF_8);
    }

    private static int parseContentLength(String headers) {
        int pos = 0;
        int headersLength = headers.length();

        while (pos < headersLength) {
            int lineEnd = headers.indexOf("\r\n", pos);
            if (lineEnd < 0) {
                lineEnd = headersLength; // 마지막 줄
            }

            // "content-length:" 대소문자 무시 비교 (15자)
            if (regionMatchesIgnoreCase(headers, pos, "content-length:", 15)) {
                int colonIdx = headers.indexOf(':', pos);
                if (colonIdx < 0 || colonIdx >= lineEnd) {
                    pos = lineEnd + 2;
                    continue;
                }

                // 콜론 다음부터 값 시작 (공백 제거)
                int valueStart = colonIdx + 1;
                while (valueStart < lineEnd && headers.charAt(valueStart) == ' ') {
                    valueStart++;
                }

                // 값 끝 (공백 제거)
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

            pos = lineEnd + 2; // \r\n 스킵
        }
        return -1;
    }

    private static boolean isChunked(String headers) {
        int pos = 0;
        int headersLength = headers.length();

        while (pos < headersLength) {
            int lineEnd = headers.indexOf("\r\n", pos);
            if (lineEnd < 0) {
                lineEnd = headersLength; // 마지막 줄
            }

            // "transfer-encoding:" 대소문자 무시 비교 (18자)
            if (regionMatchesIgnoreCase(headers, pos, "transfer-encoding:", 18)) {
                // 해당 줄에서 "chunked" 찾기 (대소문자 무시)
                for (int i = pos + 18; i <= lineEnd - 7; i++) {
                    if (regionMatchesIgnoreCase(headers, i, "chunked", 7)) {
                        return true;
                    }
                }
            }

            pos = lineEnd + 2; // \r\n 스킵
        }
        return false;
    }

    private static boolean regionMatchesIgnoreCase(String str, int offset, String target, int length) {
        if (offset + length > str.length() || length != target.length()) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            char c1 = str.charAt(offset + i);
            char c2 = target.charAt(i);
            if (c1 != c2 && Character.toLowerCase(c1) != Character.toLowerCase(c2)) {
                return false;
            }
        }
        return true;
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

    public static ByteBuffer createResponseBuffer(ResponseEntity<?> res, ByteBufferPool pool) {
        if (res == null) return null;

        byte[] bodyBytes = res.getBody() != null
                ? res.getBody().toString().getBytes(StandardCharsets.UTF_8)
                : new byte[0];

        StringBuilder header = new StringBuilder();
        header.append("HTTP/1.1 ")
                .append(res.getStatusCode().getCode())
                .append(" ")
                .append(res.getStatusCode().getMessage())
                .append("\r\n");

        header.append("Content-Type: ")
                .append(res.getContentType())
                .append("\r\n");

        header.append("Content-Length: ")
                .append(bodyBytes.length)
                .append("\r\n");

        header.append("Connection: keep-alive\r\n");
        header.append("Keep-Alive: timeout=5, max=1000\r\n");

        if (res.getHeaders() != null) {
            for (Map.Entry<String, String> entry : res.getHeaders().entrySet()) {
                header.append(entry.getKey())
                        .append(": ")
                        .append(entry.getValue())
                        .append("\r\n");
            }
        }

        header.append("\r\n");

        byte[] headerBytes = header.toString().getBytes(StandardCharsets.UTF_8);
        int totalSize = headerBytes.length + bodyBytes.length;

        // 풀에서 버퍼 대여
        ByteBuffer buffer = pool.acquire(totalSize);
        buffer.put(headerBytes);
        buffer.put(bodyBytes);
        buffer.flip();
        return buffer;
    }

}
