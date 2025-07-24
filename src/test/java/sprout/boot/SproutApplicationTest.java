package sprout.boot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import sprout.beans.annotation.ComponentScan;
import sprout.config.AppConfig;
import sprout.context.builtins.SproutApplicationContext;
import sprout.server.HttpServer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SproutApplicationTest {

    @ComponentScan(value = {"a.b"}, basePackages = {"c.d", "e.f"})
    static class WithScan {}
    static class NoScan {}

    @Test
    @DisplayName("@ComponentScan 있으면 해당 패키지 스캔 + 설정 포트로 start")
    void run_withComponentScan() throws Exception {
        HttpServer server = mock(HttpServer.class);
        AppConfig config = mock(AppConfig.class);
        when(config.getIntProperty(eq("server.port"), eq(8080))).thenReturn(9090);

        AtomicReference<String[]> capturedPkgs = new AtomicReference<>();

        try (MockedConstruction<SproutApplicationContext> mocked =
                     mockConstruction(SproutApplicationContext.class, (mock, ctx) -> {
                         capturedPkgs.set((String[]) ctx.arguments().get(0));
                         when(mock.getBean(HttpServer.class)).thenReturn(server);
                         when(mock.getBean(AppConfig.class)).thenReturn(config);
                     })) {

            SproutApplication.run(WithScan.class);

            SproutApplicationContext ctxMock = mocked.constructed().get(0);

            assertThat(capturedPkgs.get())
                    .containsExactlyInAnyOrder("a.b", "c.d", "e.f");

            verify(ctxMock).refresh();
            verify(server).start(9090);
        }
    }

    @Test
    @DisplayName("@ComponentScan 없으면 기본 패키지 스캔 + 기본 포트 8080")
    void run_withoutComponentScan() throws Exception {
        HttpServer server = mock(HttpServer.class);
        AppConfig config = mock(AppConfig.class);
        when(config.getIntProperty(eq("server.port"), eq(8080))).thenReturn(8080);

        AtomicReference<String[]> capturedPkgs = new AtomicReference<>();

        try (MockedConstruction<SproutApplicationContext> mocked =
                     mockConstruction(SproutApplicationContext.class, (mock, ctx) -> {
                         capturedPkgs.set((String[]) ctx.arguments().get(0));
                         when(mock.getBean(HttpServer.class)).thenReturn(server);
                         when(mock.getBean(AppConfig.class)).thenReturn(config);
                     })) {

            SproutApplication.run(NoScan.class);

            SproutApplicationContext ctxMock = mocked.constructed().get(0);

            assertThat(capturedPkgs.get())
                    .containsExactly(NoScan.class.getPackage().getName());

            verify(ctxMock).refresh();
            verify(server).start(8080);
        }
    }
}
