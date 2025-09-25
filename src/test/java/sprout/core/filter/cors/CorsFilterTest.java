package sprout.core.filter.cors;

import org.junit.jupiter.api.*;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import sprout.config.AppConfig;
import sprout.core.filter.FilterChain;
import sprout.mvc.http.HttpMethod;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.HttpResponse;

import java.io.IOException;
import java.util.Map;

import static org.mockito.Mockito.*;

class CorsFilterTest {

    CorsFilter corsFilter;

    @Mock HttpRequest  request;
    @Mock HttpResponse response;
    @Mock FilterChain  chain;
    @Mock AppConfig    appConfig;

    AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        corsFilter = new CorsFilter(appConfig);
        when(appConfig.getStringProperty(eq("cors.allow-origin"), anyString())).thenReturn("*");
        when(appConfig.getStringProperty(eq("cors.allow-credentials"), anyString())).thenReturn("false");
        when(appConfig.getStringProperty(eq("cors.allow-methods"), anyString())).thenReturn("GET, POST, PUT, DELETE, OPTIONS");
        when(appConfig.getStringProperty(eq("cors.allow-headers"), anyString())).thenReturn("Content-Type, Authorization");
        when(appConfig.getStringProperty(eq("cors.expose-headers"), anyString())).thenReturn("");
        when(appConfig.getStringProperty(eq("cors.max-age"), anyString())).thenReturn("3600");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) mocks.close();
    }

    // --- 작은 헬퍼: request.getHeaders()를 항상 목으로 주입 ---
    @SuppressWarnings("unchecked")
    private Map<String, Object> headers() {
        Map<String, Object> h = (Map<String, Object>) mock(Map.class);
        when(request.getHeaders()).thenReturn(h);
        return h;
    }

    @Nested
    @DisplayName("Origin 헤더가 없는 요청")
    class NoOrigin {
        @Test
        void skipsCorsAndForwardsChain() throws IOException {
            Map<String, Object> h = headers();
            when(h.get("Origin")).thenReturn(null);
            when(request.getMethod()).thenReturn(HttpMethod.GET);

            corsFilter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
            verify(response, never()).addHeader(eq("Access-Control-Allow-Origin"), anyString());
        }
    }

    @Nested
    @DisplayName("OPTIONS pre-flight 요청")
    class Options {
        @Test
        @DisplayName("Max-Age와 Allow-*를 설정하고 단락한다")
        void handlesOptionsPreflight() throws IOException {
            Map<String, Object> h = headers();
            when(h.get("Origin")).thenReturn("https://app.example.com");
            when(h.get("Access-Control-Request-Method")).thenReturn("PATCH");
            when(h.get("Access-Control-Request-Headers")).thenReturn("X-Trace-Id, Authorization");
            when(request.getMethod()).thenReturn(HttpMethod.OPTIONS);

            corsFilter.doFilter(request, response, chain);

            verify(response).addHeader("Vary","Origin");
            verify(response).addHeader("Vary","Access-Control-Request-Method");
            verify(response).addHeader("Vary","Access-Control-Request-Headers");
            verify(response).addHeader("Access-Control-Allow-Origin","*");
            verify(response).addHeader("Access-Control-Allow-Methods","PATCH");
            verify(response).addHeader("Access-Control-Allow-Headers","X-Trace-Id, Authorization");
            verify(response).addHeader("Access-Control-Max-Age","3600");
            verify(response).addHeader("Content-Length","0");
            verifyNoInteractions(chain);
        }
    }

    @Nested
    @DisplayName("일반 HTTP 요청")
    class NonOptions {
        @Test
        @DisplayName("CORS 헤더를 추가하고 체인을 계속 진행한다")
        void addsHeadersAndForwards() throws IOException {
            Map<String, Object> h = headers();
            when(h.get("Origin")).thenReturn("https://app.example.com");
            when(request.getMethod()).thenReturn(HttpMethod.GET);

            corsFilter.doFilter(request, response, chain);

            InOrder in = inOrder(response, chain);
            in.verify(response).addHeader("Vary","Origin");
            in.verify(response).addHeader("Vary","Access-Control-Request-Method");
            in.verify(response).addHeader("Vary","Access-Control-Request-Headers");
            in.verify(response).addHeader("Access-Control-Allow-Origin","*");
            in.verify(response).addHeader("Access-Control-Allow-Methods","GET, POST, PUT, DELETE, OPTIONS");
            in.verify(response).addHeader("Access-Control-Allow-Headers","Content-Type, Authorization");
            in.verify(chain).doFilter(request, response);

            verify(response, never()).addHeader(eq("Access-Control-Max-Age"), anyString());
        }
    }

    @Nested
    @DisplayName("Credentials 허용 시")
    class Credentials {
        @Test
        @DisplayName("와일드카드 대신 요청 Origin을 반사한다")
        void reflectsOriginWhenCredentialsTrue() throws IOException {
            when(appConfig.getStringProperty("cors.allow-credentials","false")).thenReturn("true");
            when(appConfig.getStringProperty("cors.allow-origin","*")).thenReturn("*");

            Map<String, Object> h = headers();
            when(h.get("Origin")).thenReturn("https://secure.example.com");
            when(request.getMethod()).thenReturn(HttpMethod.GET);

            corsFilter.doFilter(request, response, chain);

            verify(response).addHeader("Access-Control-Allow-Origin","https://secure.example.com");
            verify(response).addHeader("Access-Control-Allow-Credentials","true");
            verify(chain).doFilter(request, response);
        }
    }
}
