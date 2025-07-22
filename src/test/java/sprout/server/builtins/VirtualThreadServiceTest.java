package sprout.server.builtins;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import sprout.context.ContextPropagator;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VirtualThreadServiceTest {

    @Mock
    private List<ContextPropagator> mockPropagators;

    @Mock
    private ExecutorService mockExecutorService;

    private VirtualThreadService virtualThreadService;

    @Test
    @DisplayName("execute는 ContextSnapshot으로 작업을 감싸서 ExecutorService에 제출해야 한다")
    void execute_shouldWrapTaskWithContextAndSubmit() {
        // given: Executors의 정적 메서드를 가로채는 MockedStatic 설정
        try (MockedStatic<Executors> mockedExecutors = Mockito.mockStatic(Executors.class)) {
            // Executors.newVirtualThreadPerTaskExecutor()가 호출되면 우리의 mockExecutorService를 반환
            mockedExecutors.when(Executors::newVirtualThreadPerTaskExecutor).thenReturn(mockExecutorService);

            // given: 테스트 대상 객체 생성
            virtualThreadService = new VirtualThreadService(Collections.emptyList());
            Runnable originalTask = () -> System.out.println("Original Task");

            // when: execute 메서드 호출
            virtualThreadService.execute(originalTask);

            // then: ExecutorService의 execute 메서드로 전달된 Runnable을 캡처
            ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
            verify(mockExecutorService).execute(runnableCaptor.capture());

            Runnable capturedRunnable = runnableCaptor.getValue();

            // 캡처된 Runnable이 null이 아닌지 확인
            assertNotNull(capturedRunnable);
            // 캡처된 Runnable이 원본 Task와 다른 객체인지 확인 (즉, 감싸졌는지 확인)
            assertNotSame(originalTask, capturedRunnable, "Task should be wrapped by ContextSnapshot");
        }
    }

    @Test
    @DisplayName("shutdown은 내부 ExecutorService의 shutdown을 호출해야 한다")
    void shutdown_shouldShutdownInternalExecutor() {
        // given: 위와 동일하게 정적 메서드 Mocking 설정
        try (MockedStatic<Executors> mockedExecutors = Mockito.mockStatic(Executors.class)) {
            mockedExecutors.when(Executors::newVirtualThreadPerTaskExecutor).thenReturn(mockExecutorService);
            virtualThreadService = new VirtualThreadService(mockPropagators);

            // when: shutdown 메서드 호출
            virtualThreadService.shutdown();

            // then: 내부 ExecutorService의 shutdown이 호출되었는지 검증
            verify(mockExecutorService).shutdown();
        }
    }
}