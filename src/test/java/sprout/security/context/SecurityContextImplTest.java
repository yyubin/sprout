package sprout.security.context;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import sprout.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;

class SecurityContextImplTest {

    @Test
    @DisplayName("getAuthentication은 생성자에 전달한 Authentication을 그대로 반환해야 한다")
    void getAuthentication_returnsSameInstance() {
        Authentication auth = Mockito.mock(Authentication.class);

        SecurityContextImpl ctx = new SecurityContextImpl(auth);

        assertSame(auth, ctx.getAuthentication());
    }

    @Test
    @DisplayName("null Authentication을 허용하면 그대로 null을 반환한다")
    void getAuthentication_whenNull() {
        SecurityContextImpl ctx = new SecurityContextImpl(null);

        assertNull(ctx.getAuthentication());
    }
}
