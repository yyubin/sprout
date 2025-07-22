package sprout.security.authentication;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import sprout.security.core.GrantedAuthority;
import sprout.security.core.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class UsernamePasswordAuthenticationTokenTest {

    @Test
    @DisplayName("1) 인증 전 생성자: principal/credentials 저장, 권한은 빈 컬렉션, authenticated=false")
    void unauthenticatedCtor() {
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken("user", "pw");

        assertEquals("user", token.getPrincipal());
        assertEquals("pw", token.getCredentials());
        assertFalse(token.isAuthenticated());
        assertTrue(token.getAuthorities().isEmpty());
    }

    @Test
    @DisplayName("2) 인증 후 생성자: principal/UserDetails, 권한 세팅, authenticated=true")
    void authenticatedCtor() {
        UserDetails user = mock(UserDetails.class);
        GrantedAuthority a1 = mock(GrantedAuthority.class);
        GrantedAuthority a2 = mock(GrantedAuthority.class);
        List<GrantedAuthority> list = new ArrayList<>(List.of(a1, a2));

        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(user, null, list);

        assertSame(user, token.getPrincipal());
        assertNull(token.getCredentials());
        assertTrue(token.isAuthenticated());
        assertEquals(2, token.getAuthorities().size());
        assertTrue(token.getAuthorities().containsAll(list));

        // 원본 리스트 변경해도 내부는 불변
        list.clear();
        assertEquals(2, token.getAuthorities().size());

        // 컬렉션 자체도 수정 불가
        Collection<? extends GrantedAuthority> authorities = token.getAuthorities();
        assertThrows(UnsupportedOperationException.class, () -> authorities.add(mock(String.valueOf(GrantedAuthority.class))));
    }

    @Test
    @DisplayName("3) setAuthenticated(true)는 예외, false는 허용")
    void setAuthenticatedBehavior() {
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken("user", "pw");

        assertThrows(IllegalArgumentException.class, () -> token.setAuthenticated(true));
        assertTrue(token.getAuthorities().isEmpty()); // 그대로
        // false로 바꾸는 것은 허용 (이미 false지만 동작 확인)
        token.setAuthenticated(false);
        assertFalse(token.isAuthenticated());
    }
}
