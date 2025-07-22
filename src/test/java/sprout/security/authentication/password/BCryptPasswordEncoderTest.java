package sprout.security.authentication.password;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BCryptPasswordEncoderTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("encode: 해시가 원문과 다르고 $2 로 시작해야 한다")
    void encode_basic() {
        String raw = "secret123!";
        String encoded = encoder.encode(raw);

        assertNotEquals(raw, encoded);
        assertTrue(encoded.startsWith("$2"), "BCrypt 해시는 $2로 시작해야 함");
        assertTrue(encoded.length() > 20);
    }

    @Test
    @DisplayName("encode: 같은 비밀번호라도 해시는 매번 달라야 한다 (salt 적용)")
    void encode_saltsDiffer() {
        String raw = "samePassword";
        String e1 = encoder.encode(raw);
        String e2 = encoder.encode(raw);
        assertNotEquals(e1, e2, "서로 다른 salt로 인해 결과 해시가 달라야 함");
    }

    @Test
    @DisplayName("matches: 올바른 비밀번호면 true, 틀리면 false")
    void matches_trueFalse() {
        String raw = "pw1234";
        String encoded = encoder.encode(raw);

        assertTrue(encoder.matches(raw, encoded));
        assertFalse(encoder.matches("wrong", encoded));
    }

    @Test
    @DisplayName("null rawPassword는 NPE 발생 (현재 구현 기준)")
    void encode_nullRaw_throwsNpe() {
        assertThrows(NullPointerException.class, () -> encoder.encode(null));
    }

    @Test
    @DisplayName("matches: encodedPassword가 null이면 NPE")
    void matches_nullEncoded_throwsNpe() {
        assertThrows(NullPointerException.class, () -> encoder.matches("raw", null));
    }
}
