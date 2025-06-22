package sprout.mvc.argument.builtins;

import app.exception.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper; // ObjectMapper import
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import sprout.mvc.annotation.RequestBody;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.ResponseCode;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class RequestBodyArgumentResolverTest {

    private RequestBodyArgumentResolver resolver;
    private ObjectMapper objectMapperForTest; // 테스트용으로 JSON 문자열 생성에만 사용

    @Mock
    private HttpRequest<String> mockRequest; // HttpRequest<String> 타입으로 Mocking

    // 테스트용 더미 데이터 객체
    static class User {
        public String username;
        public int age;
        public List<String> roles;

        public User() {} // Jackson 기본 생성자 필요

        public User(String username, int age, List<String> roles) {
            this.username = username;
            this.age = age;
            this.roles = roles;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            User user = (User) o;
            return age == user.age &&
                    java.util.Objects.equals(username, user.username) &&
                    java.util.Objects.equals(roles, user.roles);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(username, age, roles);
        }

        @Override
        public String toString() {
            return "User{" +
                    "username='" + username + '\'' +
                    ", age=" + age +
                    ", roles=" + roles +
                    '}';
        }
    }

    // 테스트용 더미 컨트롤러
    private static class TestController {
        public void handleUser(@RequestBody User user) {}
        public void handleMap(@RequestBody Map<String, Object> data) {}
        public void handleString(@RequestBody String id) {} // JSON String을 직접 받을 수 있음
        public void handleNoRequestBody(String param) {}
        public void handleList(@RequestBody List<Map<String, Object>> items) {} // List<Map> 타입 핸들러 추가
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // resolver는 내부적으로 ObjectMapper를 생성하므로, 여기서는 직접 주입하지 않습니다.
        resolver = new RequestBodyArgumentResolver();
        objectMapperForTest = new ObjectMapper(); // 테스트 데이터를 JSON 문자열로 변환하는 용도
    }

    @Test
    @DisplayName("RequestBody 어노테이션이 있는 파라미터를 지원해야 한다")
    void supports_RequestBodyAnnotatedParameter_ReturnsTrue() throws NoSuchMethodException {
        Method method = TestController.class.getMethod("handleUser", User.class);
        Parameter parameter = method.getParameters()[0];

        assertThat(resolver.supports(parameter)).isTrue();
    }

    @Test
    @DisplayName("RequestBody 어노테이션이 없는 파라미터를 지원하지 않아야 한다")
    void supports_NonRequestBodyParameter_ReturnsFalse() throws NoSuchMethodException {
        Method method = TestController.class.getMethod("handleNoRequestBody", String.class);
        Parameter parameter = method.getParameters()[0];

        assertThat(resolver.supports(parameter)).isFalse();
    }

    // resolve() 메서드 테스트

    @Test
    @DisplayName("HTTP 요청 바디가 null이거나 빈 문자열일 경우 null을 반환해야 한다")
    void resolve_NullOrBlankRequestBody_ReturnsNull() throws Exception {
        Method method = TestController.class.getMethod("handleUser", User.class);
        Parameter parameter = method.getParameters()[0];

        // Case 1: null 바디
        when(mockRequest.getBody()).thenReturn(null);
        Object resolvedValueNull = resolver.resolve(parameter, mockRequest, Collections.emptyMap());
        assertThat(resolvedValueNull).isNull();

        // Case 2: 빈 문자열 바디
        when(mockRequest.getBody()).thenReturn("");
        Object resolvedValueEmpty = resolver.resolve(parameter, mockRequest, Collections.emptyMap());
        assertThat(resolvedValueEmpty).isNull();

        // Case 3: 공백 문자열 바디
        when(mockRequest.getBody()).thenReturn("   ");
        Object resolvedValueBlank = resolver.resolve(parameter, mockRequest, Collections.emptyMap());
        assertThat(resolvedValueBlank).isNull();
    }


    @Test
    @DisplayName("객체 타입의 RequestBody를 올바르게 해석하고 변환해야 한다")
    void resolve_ObjectRequestBody_ReturnsCorrectObject() throws Exception {
        User user = new User("testuser", 30, List.of("admin", "user"));
        String jsonBody = objectMapperForTest.writeValueAsString(user); // 객체를 JSON 문자열로 변환

        when(mockRequest.getBody()).thenReturn(jsonBody);

        Method method = TestController.class.getMethod("handleUser", User.class);
        Parameter parameter = method.getParameters()[0];

        User expectedUser = new User("testuser", 30, List.of("admin", "user"));

        Object resolvedValue = resolver.resolve(parameter, mockRequest, Collections.emptyMap());

        assertThat(resolvedValue).isNotNull();
        assertThat(resolvedValue).isInstanceOf(User.class);
        assertThat(resolvedValue).isEqualTo(expectedUser);
    }

    @Test
    @DisplayName("Map 타입의 RequestBody를 올바르게 해석해야 한다")
    void resolve_MapRequestBody_ReturnsCorrectMap() throws Exception {
        Map<String, Object> requestBodyData = new HashMap<>();
        requestBodyData.put("key1", "value1");
        requestBodyData.put("key2", 123);
        String jsonBody = objectMapperForTest.writeValueAsString(requestBodyData); // Map을 JSON 문자열로 변환

        when(mockRequest.getBody()).thenReturn(jsonBody);

        Method method = TestController.class.getMethod("handleMap", Map.class);
        Parameter parameter = method.getParameters()[0];

        Object resolvedValue = resolver.resolve(parameter, mockRequest, Collections.emptyMap());

        assertThat(resolvedValue).isNotNull();
        assertThat(resolvedValue).isInstanceOf(Map.class);
        assertThat((Map<String, Object>) resolvedValue).containsEntry("key1", "value1");
        assertThat((Map<String, Object>) resolvedValue).containsEntry("key2", 123);
    }

    @Test
    @DisplayName("String 타입의 RequestBody를 올바르게 해석하고 변환해야 한다 (raw JSON String)")
    void resolve_StringRequestBody_ReturnsCorrectString() throws Exception {
        // ObjectMapper는 원시 JSON 문자열을 String 타입으로 역직렬화할 때, JSON 문자열 자체의 따옴표를 제거합니다.
        String rawJsonStringAsBody = "\"simple string value\""; // HTTP 바디로 전송될 JSON 문자열 (따옴표 포함)
        String expectedString = "simple string value"; // ObjectMapper가 디코딩한 후의 순수 문자열

        when(mockRequest.getBody()).thenReturn(rawJsonStringAsBody);

        Method method = TestController.class.getMethod("handleString", String.class);
        Parameter parameter = method.getParameters()[0];

        Object resolvedValue = resolver.resolve(parameter, mockRequest, Collections.emptyMap());

        assertThat(resolvedValue).isNotNull();
        assertThat(resolvedValue).isInstanceOf(String.class);
        assertThat((String) resolvedValue).isEqualTo(expectedString);
    }

    @Test
    @DisplayName("유효하지 않은 JSON 형식일 때 BadRequestException을 던져야 한다")
    void resolve_InvalidJsonFormat_ThrowsBadRequestException() throws NoSuchMethodException {
        String invalidJsonBody = "{name:\"test\"}"; // 유효하지 않은 JSON (키에 따옴표 없음)

        when(mockRequest.getBody()).thenReturn(invalidJsonBody);

        Method method = TestController.class.getMethod("handleUser", User.class);
        Parameter parameter = method.getParameters()[0];

        assertThrows(BadRequestException.class, () ->
                resolver.resolve(parameter, mockRequest, Collections.emptyMap())
        );
    }

    @Test
    @DisplayName("JSON 데이터의 필드 타입이 대상 객체 타입과 일치하지 않을 때 BadRequestException을 던져야 한다")
    void resolve_MismatchedFieldTypes_ThrowsBadRequestException() throws NoSuchMethodException {
        String invalidBodyJson = "{\"username\":\"testuser\", \"age\":\"not-an-age\"}"; // age가 String

        when(mockRequest.getBody()).thenReturn(invalidBodyJson);

        Method method = TestController.class.getMethod("handleUser", User.class);
        Parameter parameter = method.getParameters()[0];

        assertThrows(BadRequestException.class, () ->
                resolver.resolve(parameter, mockRequest, Collections.emptyMap())
        );
    }

    @Test
    @DisplayName("JSON 바디가 비어있는 JSON 객체일 경우 대상 객체를 기본값으로 초기화해야 한다")
    void resolve_EmptyJsonBody_ReturnsDefaultInitializedObject() throws Exception {
        String emptyJsonBody = "{}";

        when(mockRequest.getBody()).thenReturn(emptyJsonBody);

        Method method = TestController.class.getMethod("handleUser", User.class);
        Parameter parameter = method.getParameters()[0];

        User expectedUser = new User(null, 0, null); // 기본 생성자로 초기화된 User 객체

        Object resolvedValue = resolver.resolve(parameter, mockRequest, Collections.emptyMap());

        assertThat(resolvedValue).isNotNull();
        assertThat(resolvedValue).isInstanceOf(User.class);
        assertThat(resolvedValue).isEqualTo(expectedUser);
    }

    @Test
    @DisplayName("JSON 배열 바디를 객체 타입으로 변환 시 BadRequestException을 던져야 한다")
    void resolve_JsonArrayBody_ThrowsBadRequestExceptionForObjectTarget() throws NoSuchMethodException {
        String jsonArrayBody = "[{\"item\":\"apple\"}, {\"item\":\"banana\"}]"; // JSON 배열

        when(mockRequest.getBody()).thenReturn(jsonArrayBody);

        Method method = TestController.class.getMethod("handleUser", User.class); // User 객체로 변환 시도
        Parameter parameter = method.getParameters()[0];

        assertThrows(BadRequestException.class, () ->
                resolver.resolve(parameter, mockRequest, Collections.emptyMap())
        );
    }

    @Test
    @DisplayName("JSON 배열 바디를 List<Map> 타입으로 변환 시 올바르게 파싱되어야 한다")
    void resolve_JsonArrayBody_ParsesAsListOfMap() throws Exception {
        String jsonArrayBody = "[{\"item\":\"apple\", \"price\":100}, {\"item\":\"banana\", \"price\":200}]";

        when(mockRequest.getBody()).thenReturn(jsonArrayBody);

        Method method = TestController.class.getMethod("handleList", List.class);
        Parameter parameter = method.getParameters()[0];

        Object resolvedValue = resolver.resolve(parameter, mockRequest, Collections.emptyMap());

        assertThat(resolvedValue).isNotNull();
        assertThat(resolvedValue).isInstanceOf(List.class);
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) resolvedValue;
        assertThat(resultList).hasSize(2);
        assertThat(resultList.get(0)).containsEntry("item", "apple").containsEntry("price", 100);
        assertThat(resultList.get(1)).containsEntry("item", "banana").containsEntry("price", 200);
    }
}