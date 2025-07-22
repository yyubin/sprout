package sprout.security.context;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import sprout.config.AppConfig;
import sprout.context.BeanFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityContextInitializerTest {

    @Test
    @DisplayName("BeanFactory에서 AppConfig를 받아 SecurityContextHolder.initialize 를 호출한다")
    void initialize_callsSecurityContextHolder() {
        BeanFactory factory = mock(BeanFactory.class);
        AppConfig appConfig = mock(AppConfig.class);

        when(factory.getBean(AppConfig.class)).thenReturn(appConfig);

        SecurityContextInitializer initializer = new SecurityContextInitializer();

        try (MockedStatic<SecurityContextHolder> holder = mockStatic(SecurityContextHolder.class)) {
            initializer.initializeAfterRefresh(factory);

            holder.verify(() -> SecurityContextHolder.initialize(appConfig));
        }
    }

    @Test
    @DisplayName("AppConfig를 못 찾으면 예외가 발생한다 (현재 구현 기준)")
    void initialize_noAppConfig_throws() {
        BeanFactory factory = mock(BeanFactory.class);
        when(factory.getBean(AppConfig.class))
                .thenThrow(new RuntimeException("No bean"));

        SecurityContextInitializer initializer = new SecurityContextInitializer();

        assertThrows(RuntimeException.class,
                () -> initializer.initializeAfterRefresh(factory));
    }
}
