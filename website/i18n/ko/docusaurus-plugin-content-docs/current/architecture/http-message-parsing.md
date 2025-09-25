# 💬 HTTP Message Parsing

## 개요

이 문서는 Sprout Framework의 HTTP 메시지 파싱 시스템에 대한 기술적 분석을 제공합니다. 원시 HTTP 요청 텍스트를 구조화된 `HttpRequest` 객체로 변환하는 파싱 파이프라인의 내부 구조, 알고리즘, 설계 결정을 검토합니다.

## 파싱 파이프라인 아키텍처

### 전체 파싱 흐름

```
원시 HTTP 텍스트 → HttpRequestParser → 구조화된 HttpRequest 객체
                        ↓
    ┌─────────────────────────────────────────────────────────┐
    │ RequestLineParser → HttpHeaderParser → QueryStringParser │
    └─────────────────────────────────────────────────────────┘
```

### 구성 요소별 책임 분리

각 파서는 HTTP 메시지의 특정 부분을 담당하는 단일 책임 원칙을 따르도록 설계 되었습니다.

- **HttpRequestParser**: 전체 조정 및 메시지 분할
- **RequestLineParser**: 요청 라인 (메서드, 경로, HTTP 버전)
- **HttpHeaderParser**: HTTP 헤더 파싱
- **QueryStringParser**: URL 쿼리 스트링 파라미터

## 핵심 구성 요소 분석

### 1. HttpRequestParser: 마스터 조정자

**메시지 분할 알고리즘**

```java
private String[] split(String raw) {
    // 1. CRLF 구분자 우선 탐색 (\r\n\r\n)
    int delimiterIdx = raw.indexOf("\r\n\r\n");
    int delimiterLen = 4;

    // 2. CRLF 없으면 LF 구분자 탐색 (\n\n)
    if (delimiterIdx == -1) {
        delimiterIdx = raw.indexOf("\n\n");
        delimiterLen = 2;
    }

    // 3. 구분자 기준으로 헤더/바디 분할
    if (delimiterIdx != -1) {
        return new String[]{
            raw.substring(0, delimiterIdx),           // 헤더 + 요청라인
            raw.substring(delimiterIdx + delimiterLen) // 바디
        };
    }
    
    // 4. 구분자 없으면 바디는 빈 문자열
    return new String[]{ raw, "" };
}
```

**설계 결정 분석**

1. **관대한 구분자 처리**: CRLF와 LF 모두 지원하여 다양한 클라이언트와의 호환성 확보
2. **조기 실패 방지**: 구분자가 없어도 전체 요청을 헤더로 처리
3. **메모리 효율성**: `substring()` 사용으로 불필요한 복사 최소화

**파싱 조정 로직**

```java
public HttpRequest<?> parse(String raw) {
    String[] parts = split(raw);
    String headerAndRequestLinePart = parts[0];
    String bodyPart = parts[1];
    
    // 첫 번째 라인 추출 (요청 라인)
    String firstLine = headerAndRequestLinePart.split("\r?\n", 2)[0];
    
    // 각 파서에 위임
    var rl = lineParser.parse(firstLine);
    var query = qsParser.parse(rl.rawPath());
    
    // 헤더 부분 추출 및 파싱
    String rawHeadersOnly = extractHeaders(headerAndRequestLinePart);
    Map<String, String> headers = headerParser.parse(rawHeadersOnly);
    
    return new HttpRequest<>(rl.method(), rl.cleanPath(), bodyPart, query, headers);
}
```

### 2. RequestLineParser: HTTP 요청 라인 파싱

**파싱 알고리즘**

```java
public RequestLine parse(String line) {
    String[] parts = line.trim().split(" ", 3);  // 최대 3개 부분으로 분할
    
    if (parts.length < 2) {
        throw new BadRequestException(ExceptionMessage.BAD_REQUEST, ResponseCode.BAD_REQUEST);
    }
    
    HttpMethod method = HttpMethod.valueOf(parts[0].toUpperCase());
    String rawPath = parts[1];
    String cleanPath = rawPath.split("\\?")[0];  // 쿼리 스트링 제거
    
    return new RequestLine(method, rawPath, cleanPath);
}
```

**핵심 설계 특징**

1. **제한된 분할**: `split(" ", 3)`로 HTTP 버전에 공백이 있어도 안전하게 처리
2. **대소문자 정규화**: HTTP 메서드를 대문자로 통일
3. **경로 전처리**: 쿼리 스트링을 분리하여 clean path 생성
4. **유효성 검증**: 최소 요구사항(메서드, 경로) 확인

**성능 최적화**

- 한 번의 `split` 호출로 모든 부분 추출
- `trim()`으로 선행/후행 공백 제거
- 예외 처리를 통한 조기 실패

### 3. HttpHeaderParser: HTTP 헤더 파싱

**정규식 기반 파싱**

```java
private static final Pattern HEADER_PATTERN = Pattern.compile("^([^:]+):\\s*(.*)$");

public Map<String, String> parse(String rawHeaders) {
    Map<String, String> headers = new HashMap<>();
    String[] lines = rawHeaders.split("\r?\n");  // 관대한 라인 분리
    
    for (String line : lines) {
        if (line.isBlank()) continue;  // 빈 라인 건너뛰기
        
        Matcher matcher = HEADER_PATTERN.matcher(line);
        if (matcher.matches()) {
            String key = matcher.group(1).trim();
            String value = matcher.group(2).trim();
            headers.put(key, value);
        } else {
            // 유효하지 않은 헤더 형식 경고
            System.err.println("Warning: Invalid header format: " + line);
        }
    }
    return headers;
}
```

**정규식 패턴 분석**

- `^([^:]+)`: 콜론 이전의 모든 문자 (헤더 이름)
- `:\\s*`: 콜론과 선택적 공백
- `(.*)$`: 나머지 모든 문자 (헤더 값)

**오류 처리 전략**

1. **유연한 라인 종료**: `\r?\n`로 CRLF/LF 모두 지원
2. **빈 라인 무시**: HTTP 스펙에 따른 빈 라인 건너뛰기  
3. **부분적 실패 허용**: 잘못된 헤더가 있어도 파싱 계속
4. **디버깅 지원**: 잘못된 형식에 대한 경고 메시지

### 4. QueryStringParser: URL 파라미터 파싱

**URL 디코딩과 파라미터 추출**

```java
public Map<String,String> parse(String rawPath) {
    Map<String,String> out = new HashMap<>();
    String[] parts = rawPath.split("\\?", 2);  // 경로와 쿼리스트링 분리
    
    if (parts.length == 2) {
        for (String token : parts[1].split("&")) {  // 파라미터별 분할
            String[] kv = token.split("=", 2);      // 키=값 분할
            
            if (kv.length == 2) {
                // 정상적인 키=값 쌍
                out.put(
                    URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(kv[1], StandardCharsets.UTF_8)
                );
            } else if (kv.length == 1 && !kv[0].isEmpty()) {
                // 값 없는 파라미터 (예: ?flag&other=value)
                out.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), "");
            }
        }
    }
    return out;
}
```

**URL 디코딩 처리**

- **문자 인코딩**: UTF-8 강제 사용으로 일관성 보장
- **안전한 디코딩**: `URLDecoder.decode()` 사용
- **빈 값 처리**: 값이 없는 파라미터도 지원

**엣지 케이스 처리**

1. **쿼리스트링 없음**: `split` 결과가 1개일 때 빈 맵 반환
2. **빈 파라미터**: 빈 키는 무시, 빈 값은 허용
3. **특수 문자**: URL 인코딩된 문자들 올바르게 디코딩

## 데이터 구조와 타입 안전성

### RequestLine Record 활용

```java
public record RequestLine(HttpMethod method, String rawPath, String cleanPath) {}
```

**Record 사용의 장점**

1. **불변성**: 생성 후 수정 불가능한 데이터 구조
2. **자동 구현**: `equals()`, `hashCode()`, `toString()` 자동 생성
3. **타입 안전성**: 컴파일 시간에 타입 검증
4. **메모리 효율성**: 최소한의 메모리 오버헤드

### HttpRequest 구조화

```java
new HttpRequest<>(rl.method(), rl.cleanPath(), bodyPart, query, headers)
```

**구조화된 데이터의 이점**

- **타입 안전성**: 각 필드가 적절한 타입으로 매핑
- **접근 편의성**: 구조화된 접근자를 통한 데이터 접근
- **확장 가능성**: 제네릭 타입으로 바디 타입 유연성

## 성능 분석

### 시간 복잡도

**HttpRequestParser**
- 전체 파싱: O(n) (n = 원시 메시지 길이)
- 메시지 분할: O(n) 
- 각 서브파서 호출: O(k) (k = 각 섹션 길이)

**개별 파서들**
- RequestLineParser: O(1) (고정된 분할 횟수)
- HttpHeaderParser: O(h) (h = 헤더 라인 수)
- QueryStringParser: O(p) (p = 쿼리 파라미터 수)

### 공간 복잡도

**메모리 사용 패턴**
- 원시 문자열: 입력 크기만큼 메모리 사용
- 중간 배열들: `split()` 연산으로 임시 배열 생성
- 최종 객체: 파싱된 데이터 구조체들

**최적화 기법**
- `substring()` 사용으로 문자열 복사 최소화
- 정규식 컴파일 캐싱 (static Pattern)
- 조기 종료로 불필요한 처리 방지

### 메모리 할당 패턴

```java
// 효율적인 문자열 처리
String[] parts = rawPath.split("\\?", 2);    // 최대 2개로 제한
String cleanPath = rawPath.split("\\?")[0];  // 첫 번째 부분만 사용

// 정규식 재사용
private static final Pattern HEADER_PATTERN = Pattern.compile("...");
```

## 오류 처리 및 복원력

### 계층화된 오류 처리

**레벨 1: 구문 오류 (Syntax Errors)**
```java
if (parts.length < 2) {
    throw new BadRequestException(ExceptionMessage.BAD_REQUEST, ResponseCode.BAD_REQUEST);
}
```

**레벨 2: 형식 경고 (Format Warnings)**
```java
System.err.println("Warning: Invalid header format detected: " + line);
```

**레벨 3: 관대한 처리 (Lenient Processing)**
```java
if (line.isBlank()) continue;  // 빈 라인 무시
```

### 복원력 있는 파싱

1. **부분적 실패 허용**: 일부 헤더가 잘못되어도 나머지 파싱 계속
2. **기본값 제공**: 누락된 요소에 대한 합리적 기본값
3. **다중 형식 지원**: CRLF/LF 라인 종료 모두 지원
4. **인코딩 안전성**: UTF-8 강제 사용으로 문자 인코딩 문제 방지

## HTTP 스펙 준수도

### RFC 7230/7231 준수 사항

**요청 라인 처리:**
- HTTP 메서드 대소문자 처리 ✓
- URI 경로 추출 ✓
- HTTP 버전 무시 (단순화) ⚠️

**헤더 처리:**
- 필드명:값 형식 ✓
- 선행/후행 공백 제거 ✓
- 빈 라인으로 헤더 종료 ✓
- 헤더 중복 처리 (마지막 값 사용) ⚠️

**메시지 바디:**
- Content-Length 검증 없음 ⚠️
- Transfer-Encoding 미지원 ⚠️

### 단순화된 구현

Sprout은 교육/학습 목적의 프레임워크로서 HTTP 스펙의 핵심 부분만 구현되어 있습니다.

**지원 기능**
- 기본적인 요청 라인 파싱
- 표준 헤더 형식
- URL 쿼리 파라미터
- UTF-8 인코딩

**의도적 제외**
- HTTP/2, HTTP/3 지원
- 청크 전송 인코딩
- 복잡한 인증 헤더
- 다중값 헤더 처리

## 확장성과 유지보수성

### 파서 교체 가능성

의존성 주입을 통해 각 파서를 독립적으로 교체 가능합니다.

```java
public HttpRequestParser(RequestLineParser lineParser, 
                        QueryStringParser qsParser, 
                        HttpHeaderParser headerParser) {
    // 각 파서를 독립적으로 주입
}
```

### 새로운 파싱 기능 추가

**확장 포인트**
1. **새로운 파서 추가**: 바디 파서, 인코딩 파서 등
2. **파싱 전략 변경**: 정규식 대신 상태 머신 파서
3. **검증 규칙 추가**: 더 엄격한 HTTP 스펙 준수
4. **성능 최적화**: 스트리밍 파서, 제로카피 파싱

### 테스트 가능성

각 파서가 독립적으로 테스트 가능한 순수 함수로 설계되어 있습니다.

```java
// 단위 테스트 용이성
@Test
void testValidRequestLine() {
    RequestLine result = parser.parse("GET /path HTTP/1.1");
    assertEquals(HttpMethod.GET, result.method());
    assertEquals("/path", result.cleanPath());
}
```

## 보안 고려사항

### 입력 검증

**현재 보안 메커니즘**
1. **입력 크기 제한 없음** ⚠️: DoS 공격 가능성
2. **URL 디코딩 안전**: 표준 라이브러리 사용
3. **정규식 보안**: 간단한 패턴으로 ReDoS 위험 낮음

**보안 개선 사안**
```java
// 권장: 입력 크기 제한
public HttpRequest<?> parse(String raw) {
    if (raw.length() > MAX_REQUEST_SIZE) {
        throw new RequestTooLargeException();
    }
    // ... 기존 로직
}
```

### 인젝션 공격 방지

**헤더 인젝션**
- 현재: 기본적인 형식 검증만 수행
- 개선: CRLF 인젝션 검증 필요

**경로 조작**
- 현재: 기본적인 URL 디코딩
- 개선: 경로 순회 공격 방지 필요

---

Sprout의 HTTP 파싱 시스템은 교육적 목적에 적합한 명확하고 이해하기 쉬운 구조를 제공합니다. 각 파서의 단일 책임 원칙 준수, 의존성 주입을 통한 테스트 가능성, 그리고 합리적인 성능 특성을 보여줍니다.

만약 보안 및 스펙 준수에 대해 기여를 하고 싶다면, 이슈 등록 후 작업 가능합니다.