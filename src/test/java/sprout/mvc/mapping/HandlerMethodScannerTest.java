package sprout.mvc.mapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor; // ArgumentCaptor import 추가
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import sprout.beans.annotation.Controller;
import sprout.context.Container;
import sprout.mvc.annotation.GetMapping;
import sprout.mvc.annotation.PostMapping;
import sprout.mvc.annotation.RequestMapping;
import sprout.mvc.http.HttpMethod;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List; // List import 추가
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HandlerMethodScannerTest {

    private HandlerMethodScanner scanner;

    @Mock
    private RequestMappingRegistry mockRegistry;
    @Mock
    private PathPatternResolver mockPathPatternResolver;
    @Mock
    private Container mockContainer;

    // 테스트용 더미 컨트롤러
    @Controller
    static class MyController {
        @RequestMapping(path = "/base", method = {HttpMethod.GET, HttpMethod.POST})
        public String classLevelBaseMethod() { return "classBase"; } // 클래스 레벨 RequestMapping에 대한 예시 메서드

        @GetMapping("/hello")
        public String hello() { return "hello"; }

        @PostMapping(value = "/create")
        public String create() { return "create"; }

        @RequestMapping(path = {"/custom"}, method = HttpMethod.PUT)
        public String customPut() { return "customPut"; }

        @GetMapping(value = "/user/{id}")
        public String getUser(String id) { return "user"; }

        // RequestMapping 어노테이션이 없는 일반 메서드
        public String nonHandlerMethod() { return "nonHandler"; }
    }

    @Controller
    @RequestMapping("/api/v1") // 클래스 레벨 RequestMapping
    static class ApiController {
        @GetMapping("/users")
        public String getUsers() { return "users"; }

        @PostMapping("/products")
        public String createProduct() { return "product"; }

        @GetMapping(path = "/items/{itemId}")
        public String getItem(String itemId) { return "item"; }
    }

    // @Controller 어노테이션이 없는 일반 클래스
    static class NormalClass {
        @GetMapping("/test")
        public String test() { return "test"; }
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        scanner = new HandlerMethodScanner(mockRegistry, mockPathPatternResolver, mockContainer);
    }

    @Test
    @DisplayName("Container에서 컨트롤러 빈을 찾아 핸들러 메서드를 스캔하고 등록해야 한다.")
    void scanControllers_registersHandlerMethods() throws NoSuchMethodException {
        // given
        MyController myController = new MyController();
        ApiController apiController = new ApiController();
        NormalClass normalClass = new NormalClass(); // @Controller 아님

        // Container가 반환할 빈 목록 설정
        Set<Object> beans = new HashSet<>(Arrays.asList(myController, apiController, normalClass));
        when(mockContainer.beans()).thenReturn(beans);

        // PathPatternResolver가 PathPattern 객체를 반환하도록 설정
        when(mockPathPatternResolver.resolve(anyString())).thenAnswer(invocation -> {
            String path = invocation.getArgument(0);
            return new PathPattern(path) { // 익명 클래스로 PathPattern Mocking (equals/hashCode만 사용될 것임)
                @Override public String getOriginalPattern() { return path; }
                @Override public boolean equals(Object o) {
                    if (this == o) return true;
                    if (o == null || getClass() != o.getClass()) return false;
                    PathPattern that = (PathPattern) o;
                    return path.equals(that.getOriginalPattern());
                }
                @Override public int hashCode() { return path.hashCode(); }
            };
        });

        // when
        scanner.scanControllers();

        // then
        // MyController의 메서드 검증 (각각의 register 호출이 한 번씩 발생했는지 검증)
        verify(mockRegistry, times(1)).register(eq(new PathPattern("/hello")), eq(HttpMethod.GET), eq(myController), eq(MyController.class.getMethod("hello")));
        verify(mockRegistry, times(1)).register(eq(new PathPattern("/create")), eq(HttpMethod.POST), eq(myController), eq(MyController.class.getMethod("create")));
        verify(mockRegistry, times(1)).register(eq(new PathPattern("/custom")), eq(HttpMethod.PUT), eq(myController), eq(MyController.class.getMethod("customPut")));
        verify(mockRegistry, times(1)).register(eq(new PathPattern("/user/{id}")), eq(HttpMethod.GET), eq(myController), eq(MyController.class.getMethod("getUser", String.class)));
        // 클래스 레벨 RequestMapping에 해당하는 메서드는 Method level RequestMapping이 없으므로 등록되지 않아야 함 (HandlerMethodScanner 로직에 따라)
        // 만약 classLevelBaseMethod도 등록되어야 한다면, 해당 어노테이션 정의와 로직을 확인해야 함.

        // ApiController의 메서드 검증 (클래스 레벨 @RequestMapping 적용)
        verify(mockRegistry, times(1)).register(eq(new PathPattern("/api/v1/users")), eq(HttpMethod.GET), eq(apiController), eq(ApiController.class.getMethod("getUsers")));
        verify(mockRegistry, times(1)).register(eq(new PathPattern("/api/v1/products")), eq(HttpMethod.POST), eq(apiController), eq(ApiController.class.getMethod("createProduct")));
        verify(mockRegistry, times(1)).register(eq(new PathPattern("/api/v1/items/{itemId}")), eq(HttpMethod.GET), eq(apiController), eq(ApiController.class.getMethod("getItem", String.class)));

        // Non-handler 메서드와 NormalClass의 메서드는 등록되지 않아야 함
        verify(mockRegistry, never()).register(any(PathPattern.class), any(HttpMethod.class), eq(myController), eq(MyController.class.getMethod("nonHandlerMethod")));
        verify(mockRegistry, never()).register(any(PathPattern.class), any(HttpMethod.class), eq(normalClass), any(Method.class));

        // Registry의 register 메서드가 총 몇 번 호출되었는지 확인 (4 + 3 = 7번)
        verify(mockRegistry, times(7)).register(any(PathPattern.class), any(HttpMethod.class), any(Object.class), any(Method.class));
    }

    @Test
    @DisplayName("클래스 레벨 RequestMapping의 path 속성이 우선시되어야 한다.")
    void extractBasePath_prefersPathOverValue() {
        // given
        @Controller
        @RequestMapping(path = {"/pathOnly"}, value = {"/valueOnly"})
        class TestControllerWithPathAndValue {}

        // when
        String basePath = scanner.extractBasePath(TestControllerWithPathAndValue.class);

        // then
        assertThat(basePath).isEqualTo("/pathOnly");
    }

    @Test
    @DisplayName("클래스 레벨 RequestMapping에 path나 value가 없으면 빈 문자열을 반환해야 한다.")
    void extractBasePath_noPathOrValue() {
        // given
        @Controller
        @RequestMapping // path, value 없음
        class TestControllerNoPathOrValue {}

        // when
        String basePath = scanner.extractBasePath(TestControllerNoPathOrValue.class);

        // then
        assertThat(basePath).isEqualTo("");
    }

    @Test
    @DisplayName("메서드 레벨 어노테이션에서 value 속성으로 경로를 추출해야 한다.")
    void findRequestMappingInfoExtractor_extractsFromValue() throws NoSuchMethodException {
        // given
        class TestMethod {
            @GetMapping(value = "/getValue")
            public void testMethod() {}
        }
        Method method = TestMethod.class.getMethod("testMethod");

        // when
        // private 메서드를 테스트하기 위해 리플렉션 사용 (일반적으로는 권장되지 않음)
        // 여기서는 HandlerMethodScanner 내부 로직을 직접 호출
        RequestMappingInfoExtractor extractor = callFindRequestMappingInfoExtractor(method);

        // then
        assertThat(extractor).isNotNull();
        assertThat(extractor.getPath()).isEqualTo("/getValue");
        assertThat(extractor.getHttpMethods()).containsExactly(HttpMethod.GET);
    }

    @Test
    @DisplayName("메서드 레벨 어노테이션에서 path 속성으로 경로를 추출해야 한다.")
    void findRequestMappingInfoExtractor_extractsFromPath() throws NoSuchMethodException {
        // given
        class TestMethod {
            @PostMapping(path = "/postPath")
            public void testMethod() {}
        }
        Method method = TestMethod.class.getMethod("testMethod");

        // when
        RequestMappingInfoExtractor extractor = callFindRequestMappingInfoExtractor(method);

        // then
        assertThat(extractor).isNotNull();
        assertThat(extractor.getPath()).isEqualTo("/postPath");
        assertThat(extractor.getHttpMethods()).containsExactly(HttpMethod.POST);
    }

    // private 메서드를 테스트하기 위한 헬퍼 메서드 (리플렉션 사용)
    private RequestMappingInfoExtractor callFindRequestMappingInfoExtractor(Method method) {
        try {
            Method findMethod = HandlerMethodScanner.class.getDeclaredMethod("findRequestMappingInfoExtractor", Method.class);
            findMethod.setAccessible(true); // private 메서드 접근 허용
            return (RequestMappingInfoExtractor) findMethod.invoke(scanner, method);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call findRequestMappingInfoExtractor via reflection", e);
        }
    }


    @Test
    @DisplayName("클래스 경로와 메서드 경로를 올바르게 결합해야 한다.")
    void combinePaths_correctlyCombines() throws NoSuchMethodException {
        // combinePaths는 private 메서드이므로, 리플렉션으로 호출하거나 (비추천)
        // 아니면 scanControllers를 통해 간접적으로만 테스트해야 합니다.
        // 여기서는 가독성을 위해 별도의 테스트 메서드로 직접 호출하는 방식을 사용합니다.
        Method combineMethod = null;
        try {
            combineMethod = HandlerMethodScanner.class.getDeclaredMethod("combinePaths", String.class, String.class);
            combineMethod.setAccessible(true); // private 메서드 접근 허용
        } catch (NoSuchMethodException e) {
            fail("combinePaths method not found or accessible");
        }


        // Case 1: 기본 경로 X, 메서드 경로 존재
        assertThat(invokeCombinePaths(combineMethod, "", "/method")).isEqualTo("/method");
        assertThat(invokeCombinePaths(combineMethod, "/", "/method")).isEqualTo("/method");
        assertThat(invokeCombinePaths(combineMethod, "", "method")).isEqualTo("/method"); // /가 없으면 추가

        // Case 2: 기본 경로 존재, 메서드 경로 X
        assertThat(invokeCombinePaths(combineMethod, "/base", "")).isEqualTo("/base");
        assertThat(invokeCombinePaths(combineMethod, "/base/", "")).isEqualTo("/base/"); // 기본경로가 /로 끝나면 유지
        assertThat(invokeCombinePaths(combineMethod, "/base", "/")).isEqualTo("/base");

        // Case 3: 둘 다 존재하고 적절히 결합
        assertThat(invokeCombinePaths(combineMethod, "/base", "/method")).isEqualTo("/base/method");
        assertThat(invokeCombinePaths(combineMethod, "/base/", "/method")).isEqualTo("/base/method"); // 기본경로 뒤 슬래시 제거 후 결합
        assertThat(invokeCombinePaths(combineMethod, "/base", "method")).isEqualTo("/base/method"); // 메서드 경로 앞 슬래시 추가
        assertThat(invokeCombinePaths(combineMethod, "/base/", "method")).isEqualTo("/base/method"); // 둘 다 처리

        // Case 4: 루트 경로만 있는 경우
        assertThat(invokeCombinePaths(combineMethod, "/", "/")).isEqualTo("/");
    }

    // private combinePaths 메서드를 호출하기 위한 헬퍼 메서드
    private String invokeCombinePaths(Method method, String basePath, String methodPath) {
        try {
            return (String) method.invoke(scanner, basePath, methodPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke combinePaths via reflection", e);
        }
    }


    @Test
    @DisplayName("Controller 어노테이션이 없는 클래스는 스캔에서 제외되어야 한다.")
    void scanControllers_excludesNonControllerClasses() throws NoSuchMethodException {
        // given
        NormalClass normalClass = new NormalClass();
        Set<Object> beans = Collections.singleton(normalClass);
        when(mockContainer.beans()).thenReturn(beans);

        // when
        scanner.scanControllers();

        // then
        // NormalClass 내의 어떤 메서드도 register되지 않아야 함
        verify(mockRegistry, never()).register(any(PathPattern.class), any(HttpMethod.class), any(Object.class), any(Method.class));
    }

    @Test
    @DisplayName("핸들러 메서드에 RequestMapping 어노테이션이 없으면 등록되지 않아야 한다.")
    void scanControllers_excludesNonHandlerMethods() throws NoSuchMethodException {
        // given
        MyController myController = new MyController();
        Set<Object> beans = Collections.singleton(myController);
        when(mockContainer.beans()).thenReturn(beans);

        // PathPatternResolver가 PathPattern 객체를 반환하도록 설정
        when(mockPathPatternResolver.resolve(anyString())).thenAnswer(invocation -> new PathPattern(invocation.getArgument(0)));

        // when
        scanner.scanControllers();

        // then
        // nonHandlerMethod는 @RequestMapping이 없으므로 등록되지 않아야 함
        verify(mockRegistry, never()).register(any(PathPattern.class), any(HttpMethod.class), eq(myController), eq(MyController.class.getMethod("nonHandlerMethod")));
        // 다른 핸들러 메서드들은 여전히 등록되어야 함을 검증 (총 4개)
        verify(mockRegistry, times(4)).register(any(PathPattern.class), any(HttpMethod.class), any(Object.class), any(Method.class));
    }

    @Test
    @DisplayName("클래스 레벨 RequestMapping이 빈 문자열이고 메서드 레벨 경로도 빈 문자열이면 '/'로 등록되어야 한다.")
    void scanControllers_emptyPathsDefaultToRoot() throws NoSuchMethodException {
        @Controller
        @RequestMapping // 클래스 레벨 경로 없음
        class DefaultPathController {
            @GetMapping // 메서드 레벨 경로 없음
            public String rootMethod() { return "root"; }
        }

        DefaultPathController controller = new DefaultPathController();
        Set<Object> beans = Collections.singleton(controller);
        when(mockContainer.beans()).thenReturn(beans);

        when(mockPathPatternResolver.resolve(anyString())).thenAnswer(invocation -> new PathPattern(invocation.getArgument(0)));

        scanner.scanControllers();

        // "/methodPath"가 아닌 "/"로 등록되는지 확인
        verify(mockRegistry).register(eq(new PathPattern("/")), eq(HttpMethod.GET), eq(controller), eq(DefaultPathController.class.getMethod("rootMethod")));
        verify(mockRegistry, times(1)).register(any(PathPattern.class), any(HttpMethod.class), any(Object.class), any(Method.class));
    }
}