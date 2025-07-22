package sprout.core.filter.cors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import sprout.core.filter.FilterChain;
import sprout.mvc.http.HttpMethod;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.HttpResponse;

import java.io.IOException;

import static org.mockito.Mockito.*;

class CorsFilterTest {

    CorsFilter corsFilter;

    @Mock HttpRequest  request;
    @Mock HttpResponse response;
    @Mock FilterChain  chain;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        corsFilter = new CorsFilter();
    }

    /* ---------- OPTIONS 사전 요청 ---------- */

    @Nested
    @DisplayName("OPTIONS pre‑flight 요청")
    class Options {

        @Test
        @DisplayName("Max‑Age 헤더를 추가하고 체인을 진행하지 않는다")
        void handlesOptionsWithoutCallingChain() throws IOException {
            when(request.getMethod()).thenReturn(HttpMethod.OPTIONS);

            corsFilter.doFilter(request, response, chain);

            // 공통 CORS 헤더 + Max‑Age 검증
            verify(response).addHeader("Access-Control-Allow-Origin",  "*");
            verify(response).addHeader("Access-Control-Allow-Methods","GET, POST, PUT, DELETE, OPTIONS");
            verify(response).addHeader("Access-Control-Allow-Headers","Content-Type, Authorization");
            verify(response).addHeader("Access-Control-Max-Age",       "3600");

            // 체인이 호출되지 않아야 한다
            verifyNoInteractions(chain);
        }
    }

    /* ---------- 일반(비‑OPTIONS) 요청 ---------- */

    @Nested
    @DisplayName("일반 HTTP 요청")
    class NonOptions {

        @Test
        @DisplayName("CORS 헤더를 추가한 뒤 체인을 계속 진행한다")
        void addsHeadersAndForwards() throws IOException {
            when(request.getMethod()).thenReturn(HttpMethod.GET);

            corsFilter.doFilter(request, response, chain);

            // 헤더 추가 순서까지 확인 (선택)
            InOrder in = inOrder(response, chain);
            in.verify(response).addHeader("Access-Control-Allow-Origin","*");
            in.verify(response).addHeader("Access-Control-Allow-Methods","GET, POST, PUT, DELETE, OPTIONS");
            in.verify(response).addHeader("Access-Control-Allow-Headers","Content-Type, Authorization");

            // Max‑Age 헤더는 없음
            verify(response, never()).addHeader(eq("Access-Control-Max-Age"), anyString());

            // 체인 진행
            in.verify(chain).doFilter(request, response);
        }
    }
}
