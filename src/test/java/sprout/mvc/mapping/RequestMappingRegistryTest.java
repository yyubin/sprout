package sprout.mvc.mapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sprout.mvc.http.HttpMethod;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class RequestMappingRegistryTest {

    private RequestMappingRegistry registry;

    // 테스트용 더미 컨트롤러
    static class TestController {
        public void getHomePage() {}
        public void getUserById(String id) {}
        public void postUser() {}
        public void putUser(String id) {}
        public void deleteUser(String id) {}
        public void getCategoryProduct(String category, String productId) {}
        public void getWildcardPath(String path) {} // {*path} 같은 개념은 아니지만, PathPattern으로 처리할 수 있는 형태
    }

    // 테스트용 더미 컨트롤러 인스턴스
    private TestController testController;

    @BeforeEach
    void setUp() {
        registry = new RequestMappingRegistry();
        testController = new TestController();
    }

    @Test
    @DisplayName("고정된 경로와 GET 메서드를 올바르게 등록하고 찾아야 한다.")
    void registerAndGetHandler_fixedPathGetMethod() throws NoSuchMethodException {
        // given
        PathPattern pathPattern = new PathPattern("/home");
        Method handlerMethod = testController.getClass().getMethod("getHomePage");

        // when
        registry.register(pathPattern, HttpMethod.GET, testController, handlerMethod);

        // then
        RequestMappingInfo<?> foundInfo = registry.getHandlerMethod("/home", HttpMethod.GET);
        assertNotNull(foundInfo);
        assertThat(foundInfo.pattern()).isEqualTo(pathPattern);
        assertThat(foundInfo.httpMethod()).isEqualTo(HttpMethod.GET);
        assertThat(foundInfo.controller()).isEqualTo(testController);
        assertThat(foundInfo.handlerMethod()).isEqualTo(handlerMethod);
    }

    @Test
    @DisplayName("다른 HTTP 메서드로 등록된 핸들러는 찾을 수 없어야 한다.")
    void getHandler_differentHttpMethod() throws NoSuchMethodException {
        // given
        PathPattern pathPattern = new PathPattern("/users");
        Method handlerMethod = testController.getClass().getMethod("postUser");
        registry.register(pathPattern, HttpMethod.POST, testController, handlerMethod);

        // when
        RequestMappingInfo<?> foundInfo = registry.getHandlerMethod("/users", HttpMethod.GET);

        // then
        assertNull(foundInfo);
    }

    @Test
    @DisplayName("등록되지 않은 경로의 핸들러는 찾을 수 없어야 한다.")
    void getHandler_unregisteredPath() {
        // given (아무것도 등록하지 않음)

        // when
        RequestMappingInfo<?> foundInfo = registry.getHandlerMethod("/nonexistent", HttpMethod.GET);

        // then
        assertNull(foundInfo);
    }

    @Test
    @DisplayName("경로 변수가 있는 경로를 올바르게 등록하고 찾아야 한다.")
    void registerAndGetHandler_pathVariable() throws NoSuchMethodException {
        // given
        PathPattern pathPattern = new PathPattern("/users/{id}");
        Method handlerMethod = testController.getClass().getMethod("getUserById", String.class);
        registry.register(pathPattern, HttpMethod.GET, testController, handlerMethod);

        // when
        RequestMappingInfo<?> foundInfo = registry.getHandlerMethod("/users/123", HttpMethod.GET);

        // then
        assertNotNull(foundInfo);
        assertThat(foundInfo.pattern()).isEqualTo(pathPattern); // 패턴 객체 자체가 매칭되어야 함
        assertThat(foundInfo.httpMethod()).isEqualTo(HttpMethod.GET);
        assertThat(foundInfo.controller()).isEqualTo(testController);
        assertThat(foundInfo.handlerMethod()).isEqualTo(handlerMethod);

        // 다른 ID 값도 잘 찾아야 함
        RequestMappingInfo<?> foundInfo2 = registry.getHandlerMethod("/users/abc", HttpMethod.GET);
        assertNotNull(foundInfo2);
        assertThat(foundInfo2.pattern()).isEqualTo(pathPattern);
    }

    @Test
    @DisplayName("여러 경로 변수가 있는 경로를 올바르게 등록하고 찾아야 한다.")
    void registerAndGetHandler_multiplePathVariables() throws NoSuchMethodException {
        // given
        PathPattern pathPattern = new PathPattern("/categories/{category}/products/{productId}");
        Method handlerMethod = testController.getClass().getMethod("getCategoryProduct", String.class, String.class);
        registry.register(pathPattern, HttpMethod.GET, testController, handlerMethod);

        // when
        RequestMappingInfo<?> foundInfo = registry.getHandlerMethod("/categories/books/products/B-456", HttpMethod.GET);

        // then
        assertNotNull(foundInfo);
        assertThat(foundInfo.pattern()).isEqualTo(pathPattern);
        assertThat(foundInfo.httpMethod()).isEqualTo(HttpMethod.GET);
        assertThat(foundInfo.controller()).isEqualTo(testController);
        assertThat(foundInfo.handlerMethod()).isEqualTo(handlerMethod);
    }

    @Test
    @DisplayName("동일한 경로에 여러 HTTP 메서드를 등록하고 각각 찾아야 한다.")
    void registerAndGetHandler_samePathDifferentMethods() throws NoSuchMethodException {
        // given
        PathPattern userPath = new PathPattern("/users/{id}");
        Method getUserMethod = testController.getClass().getMethod("getUserById", String.class);
        Method putUserMethod = testController.getClass().getMethod("putUser", String.class);
        Method deleteUserMethod = testController.getClass().getMethod("deleteUser", String.class);

        registry.register(userPath, HttpMethod.GET, testController, getUserMethod);
        registry.register(userPath, HttpMethod.PUT, testController, putUserMethod);
        registry.register(userPath, HttpMethod.DELETE, testController, deleteUserMethod);

        // then
        assertNotNull(registry.getHandlerMethod("/users/1", HttpMethod.GET));
        assertNotNull(registry.getHandlerMethod("/users/1", HttpMethod.PUT));
        assertNotNull(registry.getHandlerMethod("/users/1", HttpMethod.DELETE));
        assertNull(registry.getHandlerMethod("/users/1", HttpMethod.POST)); // 등록 안 된 메서드
    }

    @Test
    @DisplayName("경로 변수 개수가 적은 패턴이 먼저 매칭되면 안 되고, 정확한 매칭이 이루어져야 한다.")
    void getHandler_prefersMoreSpecificPattern() throws NoSuchMethodException {
        // given: 더 일반적인 패턴을 먼저 등록하고, 더 구체적인 패턴을 나중에 등록 (순서 무관 테스트)
        // PathPattern::getVariableCount로 정렬하기 때문에 등록 순서는 상관없지만, 테스트를 명확히 위해 명시

        // 구체적인 패턴 (변수 1개)
        PathPattern specificPattern = new PathPattern("/users/{id}");
        Method specificMethod = testController.getClass().getMethod("getUserById", String.class);
        registry.register(specificPattern, HttpMethod.GET, testController, specificMethod);

        // 더 일반적인 패턴 (고정 경로)
        PathPattern generalPattern = new PathPattern("/users");
        Method generalMethod = testController.getClass().getMethod("getHomePage"); // 예시로 다른 메서드 사용
        registry.register(generalPattern, HttpMethod.GET, testController, generalMethod);

        // when & then
        // "/users" 요청은 "/users" 패턴에 매칭되어야 함
        RequestMappingInfo<?> foundGeneral = registry.getHandlerMethod("/users", HttpMethod.GET);
        assertNotNull(foundGeneral);
        assertThat(foundGeneral.pattern()).isEqualTo(generalPattern);
        assertThat(foundGeneral.handlerMethod()).isEqualTo(generalMethod);

        // "/users/123" 요청은 "/users/{id}" 패턴에 매칭되어야 함
        RequestMappingInfo<?> foundSpecific = registry.getHandlerMethod("/users/123", HttpMethod.GET);
        assertNotNull(foundSpecific);
        assertThat(foundSpecific.pattern()).isEqualTo(specificPattern);
        assertThat(foundSpecific.handlerMethod()).isEqualTo(specificMethod);
    }

    @Test
    @DisplayName("PathPattern 정렬 로직이 예상대로 동작하는지 확인한다 (변수 개수 기준).")
    void getHandlerMethod_sortsPatternsByVariableCount() throws NoSuchMethodException {
        // given
        PathPattern p1 = new PathPattern("/api/v1/resource"); // 변수 0
        PathPattern p2 = new PathPattern("/api/v1/{id}");       // 변수 1
        PathPattern p3 = new PathPattern("/{var1}/path/{var2}"); // 변수 2
        PathPattern p4 = new PathPattern("/"); // 변수 0 (루트)

        registry.register(p1, HttpMethod.GET, testController, testController.getClass().getMethod("getHomePage"));
        registry.register(p2, HttpMethod.GET, testController, testController.getClass().getMethod("getUserById", String.class));
        registry.register(p3, HttpMethod.GET, testController, testController.getClass().getMethod("getCategoryProduct", String.class, String.class));
        registry.register(p4, HttpMethod.GET, testController, testController.getClass().getMethod("getHomePage"));


        // when: /api/v1/resource 요청 (변수 0개 패턴)
        RequestMappingInfo<?> info0 = registry.getHandlerMethod("/api/v1/resource", HttpMethod.GET);
        assertNotNull(info0);
        assertThat(info0.pattern()).isEqualTo(p1);

        // when: /api/v1/123 요청 (변수 1개 패턴)
        RequestMappingInfo<?> info1 = registry.getHandlerMethod("/api/v1/123", HttpMethod.GET);
        assertNotNull(info1);
        assertThat(info1.pattern()).isEqualTo(p2);

        // when: /val1/path/val2 요청 (변수 2개 패턴)
        RequestMappingInfo<?> info2 = registry.getHandlerMethod("/val1/path/val2", HttpMethod.GET);
        assertNotNull(info2);
        assertThat(info2.pattern()).isEqualTo(p3);

        // when: / 요청 (변수 0개 루트 패턴)
        RequestMappingInfo<?> infoRoot = registry.getHandlerMethod("/", HttpMethod.GET);
        assertNotNull(infoRoot);
        assertThat(infoRoot.pattern()).isEqualTo(p4);
    }
}