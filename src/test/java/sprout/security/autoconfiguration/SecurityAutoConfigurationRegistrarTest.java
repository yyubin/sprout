package sprout.security.autoconfiguration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import sprout.beans.BeanDefinition;
import sprout.beans.ConstructorBeanDefinition;
import sprout.security.authentication.*;
import sprout.security.authentication.password.BCryptPasswordEncoder;
import sprout.security.authentication.password.PasswordEncoder;
import sprout.security.autoconfiguration.annotation.EnableSproutSecurity;
import sprout.security.core.UserDetailsService;
import sprout.security.filter.AuthenticationFilter;
import sprout.security.web.util.matcher.AntPathRequestMatcher;
import sprout.security.web.util.matcher.RequestMatcher;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityAutoConfigurationRegistrarTest {

    private BeanDefinition mockDef(Class<?> type) {
        BeanDefinition def = mock(BeanDefinition.class, Mockito.RETURNS_DEEP_STUBS);
        doReturn(type).when(def).getType();
        return def;
    }

    @Test
    @DisplayName("@EnableSproutSecurity 없음 → 기본 보안 빈들 모두 등록")
    void register_whenNoEnableAnnotation_registerDefaults() throws Exception {
        SecurityAutoConfigurationRegistrar registrar = new SecurityAutoConfigurationRegistrar();

        Collection<BeanDefinition> result = registrar.registerAdditionalBeanDefinitions(List.of());

        // 타입만 체크
        assertContains(result, PasswordEncoder.class, BCryptPasswordEncoder.class);
        assertContains(result, UserDetailsService.class, DefaultUserDetailsService.class);
        assertContains(result, DaoAuthenticationProvider.class, DaoAuthenticationProvider.class);
        assertContains(result, RequestMatcher.class, AntPathRequestMatcher.class);
        assertContains(result, AuthenticationManager.class, ProviderManager.class);
        assertContains(result, AuthenticationFilter.class, AuthenticationFilter.class);

        // 6개 모두
        assertEquals(6, result.size());
    }

    @Test
    @DisplayName("이미 일부 빈이 존재하면 중복 등록하지 않는다")
    void skipAlreadyPresentBeans() throws Exception {
        SecurityAutoConfigurationRegistrar registrar = new SecurityAutoConfigurationRegistrar();

        // 이미 PasswordEncoder, AuthenticationManager 가 존재한다고 가정
        BeanDefinition existingEncoder = mockDef(PasswordEncoder.class);
        BeanDefinition existingManager = mockDef(AuthenticationManager.class);

        Collection<BeanDefinition> result = registrar.registerAdditionalBeanDefinitions(
                List.of(existingEncoder, existingManager)
        );

        // Encoder, Manager 는 추가 안 되고 나머지 4개만
        assertFalse(result.stream().anyMatch(bd -> PasswordEncoder.class.isAssignableFrom(bd.getType())));
        assertFalse(result.stream().anyMatch(bd -> AuthenticationManager.class.isAssignableFrom(bd.getType())));
        assertEquals(4, result.size());
    }

    @Test
    @DisplayName("@EnableSproutSecurity 존재 + defaultSecurityDisabled=false → 기본 등록 안 함")
    void enableAnnotation_disableDefaultFalse_noRegister() throws Exception {
        SecurityAutoConfigurationRegistrar registrar = new SecurityAutoConfigurationRegistrar();

        BeanDefinition cfg = mockDef(SecEnabled.class); // @EnableSproutSecurity 붙은 클래스
        Collection<BeanDefinition> result = registrar.registerAdditionalBeanDefinitions(List.of(cfg));

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("@EnableSproutSecurity(defaultSecurityDisabled=true) → 기본 등록")
    void enableAnnotation_disableDefaultTrue_register() throws Exception {
        SecurityAutoConfigurationRegistrar registrar = new SecurityAutoConfigurationRegistrar();

        BeanDefinition cfg = mockDef(SecEnabledDisabledTrue.class);
        Collection<BeanDefinition> result = registrar.registerAdditionalBeanDefinitions(List.of(cfg));
        // 6개 등록
        assertEquals(6, result.size());
    }

    private void assertContains(Collection<BeanDefinition> defs, Class<?> superType, Class<?> exactType) {
        List<BeanDefinition> filtered = defs.stream()
                .filter(bd -> superType.isAssignableFrom(bd.getType()))
                .collect(Collectors.toList());
        assertEquals(1, filtered.size(), "Expected one bean assignable to " + superType.getName());
        assertEquals(exactType, filtered.get(0).getType());
        assertTrue(filtered.get(0) instanceof ConstructorBeanDefinition);
    }

    // 테스트용 @EnableSproutSecurity 클래스들
    @EnableSproutSecurity
    static class SecEnabled {}

    @EnableSproutSecurity(defaultSecurityDisabled = true)
    static class SecEnabledDisabledTrue {}
}
