package sprout.mvc.advice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sprout.context.BeanFactory;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ControllerAdviceContextInitializerTest {

    @Mock ControllerAdviceRegistry registry;
    @Mock BeanFactory beanFactory;

    @Test
    @DisplayName("initializeAfterRefresh는 BeanFactory를 registry로 위임한다")
    void initialize_delegatesToRegistry() {
        // given
        ControllerAdviceContextInitializer initializer =
                new ControllerAdviceContextInitializer(registry);

        // when
        initializer.initializeAfterRefresh(beanFactory);

        // then
        verify(registry).scanControllerAdvices(beanFactory);
        verifyNoMoreInteractions(registry);
    }
}
