package sprout.mvc.argument.builtins;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import sprout.mvc.annotation.RequestBody;
import sprout.mvc.http.HttpRequest;

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
    private ObjectMapper objectMapperForTest; // 테스트 내부에서 객체 생성 및 JSON 문자열로 변환용

    @Mock
    private HttpRequest<Object> mockRequest; // HttpRequest<Object> 타입으로 Mocking

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
        public void handleString(@RequestBody String id) {}
        public void handleNoRequestBody(String param) {}
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        resolver = new RequestBodyArgumentResolver();
        objectMapperForTest = new ObjectMapper();
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
    @DisplayName("HTTP 요청 바디가 null일 경우 null을 반환해야 한다")
    void resolve_NullRequestBody_ReturnsNull() throws Exception {
        // null은 Object 타입이므로 캐스팅 필요 없음
        when(mockRequest.getBody()).thenReturn(null);

        Method method = TestController.class.getMethod("handleUser", User.class);
        Parameter parameter = method.getParameters()[0];

        Object resolvedValue = resolver.resolve(parameter, mockRequest, Collections.emptyMap());

        assertThat(resolvedValue).isNull();
    }

    @Test
    @DisplayName("객체 타입의 RequestBody를 올바르게 해석하고 변환해야 한다")
    void resolve_ObjectRequestBody_ReturnsCorrectObject() throws Exception {
        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("username", "testuser");
        requestBodyMap.put("age", 30);
        requestBodyMap.put("roles", List.of("admin", "user"));

        // Map<String, Object>를 Object로 캐스팅
        when(mockRequest.getBody()).thenReturn((Object) requestBodyMap);

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
        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("key1", "value1");
        requestBodyMap.put("key2", 123);

        // Map<String, Object>를 Object로 캐스팅
        when(mockRequest.getBody()).thenReturn((Object) requestBodyMap);

        Method method = TestController.class.getMethod("handleMap", Map.class);
        Parameter parameter = method.getParameters()[0];

        Object resolvedValue = resolver.resolve(parameter, mockRequest, Collections.emptyMap());

        assertThat(resolvedValue).isNotNull();
        assertThat(resolvedValue).isInstanceOf(Map.class);
        assertThat((Map<String, Object>) resolvedValue).containsEntry("key1", "value1");
        assertThat((Map<String, Object>) resolvedValue).containsEntry("key2", 123);
    }

    @Test
    @DisplayName("String 타입의 RequestBody 변환 시 IllegalArgumentException을 던져야 한다 (Map to String 변환 시도)")
    void resolve_StringRequestBody_ThrowsIllegalArgumentException() throws Exception {
        Map<String, Object> requestBodyMap = Map.of("value", "some text");

        // Map<String, Object>를 Object로 캐스팅
        when(mockRequest.getBody()).thenReturn((Object) requestBodyMap);

        Method method = TestController.class.getMethod("handleString", String.class);
        Parameter parameter = method.getParameters()[0];

        Exception thrown = assertThrows(IllegalArgumentException.class, () ->
                resolver.resolve(parameter, mockRequest, Collections.emptyMap())
        );
        assertThat(thrown.getMessage()).contains("Failed to convert request body to 'java.lang.String'");
    }

    @Test
    @DisplayName("JSON 데이터의 필드 타입이 대상 객체 타입과 일치하지 않을 때 IllegalArgumentException을 던져야 한다")
    void resolve_MismatchedFieldTypes_ThrowsIllegalArgumentException() throws NoSuchMethodException {
        Map<String, Object> invalidBodyMap = new HashMap<>();
        invalidBodyMap.put("username", "testuser");
        invalidBodyMap.put("age", "not-an-age");

        // Map<String, Object>를 Object로 캐스팅
        when(mockRequest.getBody()).thenReturn((Object) invalidBodyMap);

        Method method = TestController.class.getMethod("handleUser", User.class);
        Parameter parameter = method.getParameters()[0];

        Exception thrown = assertThrows(IllegalArgumentException.class, () -> {
            resolver.resolve(parameter, mockRequest, Collections.emptyMap());
        });

        assertThat(thrown.getMessage()).contains("Failed to convert request body to 'sprout.mvc.argument.builtins.RequestBodyArgumentResolverTest$User'");
        assertThat(thrown.getMessage()).contains("Cannot deserialize value of type `int` from String \"not-an-age\"");
    }

    @Test
    @DisplayName("JSON 바디가 null이 아닌 비어있는 Map일 경우 ObjectMapper가 처리해야 한다")
    void resolve_EmptyMapRequestBody_ReturnsEmptyObject() throws Exception {
        // Map<String, Object>를 Object로 캐스팅
        when(mockRequest.getBody()).thenReturn((Object) Collections.emptyMap());

        Method method = TestController.class.getMethod("handleUser", User.class);
        Parameter parameter = method.getParameters()[0];

        User expectedUser = new User(null, 0, null);

        Object resolvedValue = resolver.resolve(parameter, mockRequest, Collections.emptyMap());

        assertThat(resolvedValue).isNotNull();
        assertThat(resolvedValue).isInstanceOf(User.class);
        assertThat(resolvedValue).isEqualTo(expectedUser);
    }
}