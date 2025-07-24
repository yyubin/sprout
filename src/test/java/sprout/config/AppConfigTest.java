package sprout.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AppConfigTest {

    @Test
    @DisplayName("YAML 로드 & 중첩 키 조회")
    void loadAndGetProperties() {
        AppConfig cfg = new AppConfig(); // 자동으로 application.yml 로드

        assertEquals("jdbc:mysql://localhost:3306/sprout",
                cfg.getStringProperty("db.url", "default"));
        assertEquals(5, cfg.getIntProperty("db.pool.size", -1));
        assertEquals(8080, cfg.getIntProperty("server.port", -1));
    }

    @Test
    @DisplayName("없는 키면 기본값 반환")
    void missingKeyReturnsDefault() {
        AppConfig cfg = new AppConfig();

        assertEquals("fallback", cfg.getStringProperty("no.such.key", "fallback"));
        assertEquals(123, cfg.getIntProperty("no.such.int", 123));
    }

    @Test
    @DisplayName("숫자 아닌 값은 NumberFormatException 없이 기본값")
    void nonNumericFallsBack() {
        AppConfig cfg = new AppConfig();

        assertEquals(999, cfg.getIntProperty("misc.notNumber", 999));
    }
}
