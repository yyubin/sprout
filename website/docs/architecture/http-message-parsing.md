# 💬 HTTP Message Parsing

## Overview

This document provides a technical analysis of Sprout Framework's HTTP message parsing system. It examines the internal structure, algorithms, and design decisions of the parsing pipeline that transforms raw HTTP request text into structured `HttpRequest` objects.

## Parsing Pipeline Architecture

### Overall Parsing Flow

```
Raw HTTP Text → HttpRequestParser → Structured HttpRequest Object
                        ↓
    ┌─────────────────────────────────────────────────────────┐
    │ RequestLineParser → HttpHeaderParser → QueryStringParser │
    └─────────────────────────────────────────────────────────┘
```

### Separation of Responsibilities

Each parser is designed to follow the single responsibility principle, handling a specific part of the HTTP message:

- **HttpRequestParser**: Overall coordination and message splitting
- **RequestLineParser**: Request line (method, path, HTTP version)
- **HttpHeaderParser**: HTTP header parsing
- **QueryStringParser**: URL query string parameters

## Core Component Analysis

### 1. HttpRequestParser: Master Coordinator

**Message Splitting Algorithm:**

```java
private String[] split(String raw) {
    // 1. Search for CRLF delimiter first (\r\n\r\n)
    int delimiterIdx = raw.indexOf("\r\n\r\n");
    int delimiterLen = 4;

    // 2. If no CRLF, search for LF delimiter (\n\n)
    if (delimiterIdx == -1) {
        delimiterIdx = raw.indexOf("\n\n");
        delimiterLen = 2;
    }

    // 3. Split header/body based on delimiter
    if (delimiterIdx != -1) {
        return new String[]{
            raw.substring(0, delimiterIdx),           // Headers + request line
            raw.substring(delimiterIdx + delimiterLen) // Body
        };
    }
    
    // 4. If no delimiter found, body is empty string
    return new String[]{ raw, "" };
}
```

**Design Decision Analysis:**

1. **Lenient Delimiter Handling**: Supports both CRLF and LF for compatibility with various clients
2. **Failure Prevention**: Even without delimiters, treats entire request as headers
3. **Memory Efficiency**: Uses `substring()` to minimize unnecessary copying

**Parsing Coordination Logic:**

```java
public HttpRequest<?> parse(String raw) {
    String[] parts = split(raw);
    String headerAndRequestLinePart = parts[0];
    String bodyPart = parts[1];
    
    // Extract first line (request line)
    String firstLine = headerAndRequestLinePart.split("\r?\n", 2)[0];
    
    // Delegate to each parser
    var rl = lineParser.parse(firstLine);
    var query = qsParser.parse(rl.rawPath());
    
    // Extract and parse header section
    String rawHeadersOnly = extractHeaders(headerAndRequestLinePart);
    Map<String, String> headers = headerParser.parse(rawHeadersOnly);
    
    return new HttpRequest<>(rl.method(), rl.cleanPath(), bodyPart, query, headers);
}
```

### 2. RequestLineParser: HTTP Request Line Parsing

**Parsing Algorithm:**

```java
public RequestLine parse(String line) {
    String[] parts = line.trim().split(" ", 3);  // Split into maximum 3 parts
    
    if (parts.length < 2) {
        throw new BadRequestException(ExceptionMessage.BAD_REQUEST, ResponseCode.BAD_REQUEST);
    }
    
    HttpMethod method = HttpMethod.valueOf(parts[0].toUpperCase());
    String rawPath = parts[1];
    String cleanPath = rawPath.split("\\?")[0];  // Remove query string
    
    return new RequestLine(method, rawPath, cleanPath);
}
```

**Key Design Features:**

1. **Limited Splitting**: `split(" ", 3)` safely handles HTTP versions with spaces
2. **Case Normalization**: Unifies HTTP methods to uppercase
3. **Path Preprocessing**: Separates query string to create clean path
4. **Validation**: Checks minimum requirements (method, path)

**Performance Optimizations:**

- Single `split` call to extract all parts
- `trim()` removes leading/trailing whitespace
- Early failure through exception handling

### 3. HttpHeaderParser: HTTP Header Parsing

**Regex-Based Parsing:**

```java
private static final Pattern HEADER_PATTERN = Pattern.compile("^([^:]+):\\s*(.*)$");

public Map<String, String> parse(String rawHeaders) {
    Map<String, String> headers = new HashMap<>();
    String[] lines = rawHeaders.split("\r?\n");  // Lenient line separation
    
    for (String line : lines) {
        if (line.isBlank()) continue;  // Skip blank lines
        
        Matcher matcher = HEADER_PATTERN.matcher(line);
        if (matcher.matches()) {
            String key = matcher.group(1).trim();
            String value = matcher.group(2).trim();
            headers.put(key, value);
        } else {
            // Warning for invalid header format
            System.err.println("Warning: Invalid header format: " + line);
        }
    }
    return headers;
}
```

**Regex Pattern Analysis:**

- `^([^:]+)`: All characters before colon (header name)
- `:\\s*`: Colon and optional whitespace
- `(.*)$`: All remaining characters (header value)

**Error Handling Strategy:**

1. **Flexible Line Endings**: `\r?\n` supports both CRLF/LF
2. **Blank Line Skipping**: Ignores blank lines per HTTP spec
3. **Partial Failure Tolerance**: Continues parsing even with invalid headers
4. **Debug Support**: Warning messages for malformed formats

### 4. QueryStringParser: URL Parameter Parsing

**URL Decoding and Parameter Extraction:**

```java
public Map<String,String> parse(String rawPath) {
    Map<String,String> out = new HashMap<>();
    String[] parts = rawPath.split("\\?", 2);  // Separate path and query string
    
    if (parts.length == 2) {
        for (String token : parts[1].split("&")) {  // Split by parameter
            String[] kv = token.split("=", 2);      // Split key=value
            
            if (kv.length == 2) {
                // Normal key=value pair
                out.put(
                    URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(kv[1], StandardCharsets.UTF_8)
                );
            } else if (kv.length == 1 && !kv[0].isEmpty()) {
                // Parameter without value (e.g., ?flag&other=value)
                out.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), "");
            }
        }
    }
    return out;
}
```

**URL Decoding Handling:**

- **Character Encoding**: Forces UTF-8 usage for consistency
- **Safe Decoding**: Uses `URLDecoder.decode()`
- **Empty Value Handling**: Supports parameters without values

**Edge Case Handling:**

1. **No Query String**: Returns empty map when split results in 1 part
2. **Empty Parameters**: Ignores empty keys, allows empty values
3. **Special Characters**: Properly decodes URL-encoded characters

## Data Structures and Type Safety

### RequestLine Record Usage

```java
public record RequestLine(HttpMethod method, String rawPath, String cleanPath) {}
```

**Advantages of Using Records:**

1. **Immutability**: Data structure cannot be modified after creation
2. **Auto-generation**: Automatic `equals()`, `hashCode()`, `toString()` implementation
3. **Type Safety**: Compile-time type verification
4. **Memory Efficiency**: Minimal memory overhead

### HttpRequest Structuring

```java
new HttpRequest<>(rl.method(), rl.cleanPath(), bodyPart, query, headers)
```

**Benefits of Structured Data:**

- **Type Safety**: Each field mapped to appropriate type
- **Access Convenience**: Data access through structured accessors
- **Extensibility**: Generic type provides body type flexibility

## Performance Analysis

### Time Complexity

**HttpRequestParser:**
- Overall parsing: O(n) (n = raw message length)
- Message splitting: O(n)
- Each sub-parser call: O(k) (k = each section length)

**Individual Parsers:**
- RequestLineParser: O(1) (fixed number of splits)
- HttpHeaderParser: O(h) (h = number of header lines)
- QueryStringParser: O(p) (p = number of query parameters)

### Space Complexity

**Memory Usage Patterns:**
- Raw string: Memory usage equal to input size
- Intermediate arrays: Temporary arrays created by `split()` operations
- Final objects: Parsed data structures

**Optimization Techniques:**
- Uses `substring()` to minimize string copying
- Regex compilation caching (static Pattern)
- Early termination prevents unnecessary processing

### Memory Allocation Patterns

```java
// Efficient string processing
String[] parts = rawPath.split("\\?", 2);    // Limited to maximum 2
String cleanPath = rawPath.split("\\?")[0];  // Uses only first part

// Regex reuse
private static final Pattern HEADER_PATTERN = Pattern.compile("...");
```

## Error Handling and Resilience

### Layered Error Handling

**Level 1: Syntax Errors**
```java
if (parts.length < 2) {
    throw new BadRequestException(ExceptionMessage.BAD_REQUEST, ResponseCode.BAD_REQUEST);
}
```

**Level 2: Format Warnings**
```java
System.err.println("Warning: Invalid header format detected: " + line);
```

**Level 3: Lenient Processing**
```java
if (line.isBlank()) continue;  // Skip blank lines
```

### Resilient Parsing

1. **Partial Failure Tolerance**: Continues parsing even with some invalid headers
2. **Default Value Provision**: Reasonable defaults for missing elements
3. **Multiple Format Support**: Supports both CRLF/LF line endings
4. **Encoding Safety**: Forces UTF-8 usage to prevent character encoding issues

## HTTP Specification Compliance

### RFC 7230/7231 Compliance

**Request Line Processing:**
- HTTP method case handling ✓
- URI path extraction ✓
- HTTP version ignored (simplified) ⚠️

**Header Processing:**
- Field-name:value format ✓
- Leading/trailing whitespace removal ✓
- Header termination with blank line ✓
- Duplicate header handling (uses last value) ⚠️

**Message Body:**
- No Content-Length validation ⚠️
- Transfer-Encoding not supported ⚠️

### Simplified Implementation

As an educational/learning framework, Sprout implements only the core parts of the HTTP specification:

**Supported Features:**
- Basic request line parsing
- Standard header formats
- URL query parameters
- UTF-8 encoding

**Intentional Exclusions:**
- HTTP/2, HTTP/3 support
- Chunked transfer encoding
- Complex authentication headers
- Multi-value header processing

## Extensibility and Maintainability

### Parser Replaceability

Each parser can be independently replaced through dependency injection:

```java
public HttpRequestParser(RequestLineParser lineParser, 
                        QueryStringParser qsParser, 
                        HttpHeaderParser headerParser) {
    // Each parser injected independently
}
```

### Adding New Parsing Features

**Extension Points:**
1. **New Parser Addition**: Body parsers, encoding parsers, etc.
2. **Parsing Strategy Changes**: State machine parsers instead of regex
3. **Validation Rule Addition**: Stricter HTTP specification compliance
4. **Performance Optimization**: Streaming parsers, zero-copy parsing

### Testability

Each parser is designed as a pure function that can be tested independently:

```java
// Unit testing ease
@Test
void testValidRequestLine() {
    RequestLine result = parser.parse("GET /path HTTP/1.1");
    assertEquals(HttpMethod.GET, result.method());
    assertEquals("/path", result.cleanPath());
}
```

## Security Considerations

### Input Validation

**Current Security Mechanisms:**
1. **No Input Size Limits** ⚠️: Potential DoS attack vulnerability
2. **Safe URL Decoding**: Uses standard library
3. **Regex Security**: Simple patterns with low ReDoS risk

**Security Improvement Recommendations:**
```java
// Recommended: Input size limits
public HttpRequest<?> parse(String raw) {
    if (raw.length() > MAX_REQUEST_SIZE) {
        throw new RequestTooLargeException();
    }
    // ... existing logic
}
```

### Injection Attack Prevention

**Header Injection:**
- Current: Basic format validation only
- Improvement: CRLF injection validation needed

**Path Manipulation:**
- Current: Basic URL decoding
- Improvement: Path traversal attack prevention needed

---

Sprout's HTTP parsing system provides a clear and understandable structure suitable for educational purposes. It demonstrates adherence to single responsibility principle for each parser, testability through dependency injection, and reasonable performance characteristics.

If you want to contribute to security and specification compliance improvements, please register an issue before starting work.