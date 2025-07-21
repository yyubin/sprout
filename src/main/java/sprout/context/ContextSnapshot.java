package sprout.context;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

public class ContextSnapshot{

    private final List<ContextPropagator> propagators;

    // 현재 스레드의 ThreadLocal에서 모든 컨텍스트 정보를 캡처하여 스냅샷을 생성
    public ContextSnapshot(List<ContextPropagator> propagators) {
        this.propagators = propagators;
        for (ContextPropagator p : propagators) {
            p.capture();
        }
    }

    public Runnable wrap(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable cannot be null");
        return () -> {
            // 작업 실행 전, 캡처한 컨텍스트를 현재 스레드의 ThreadLocal에 설정
            try {
                for (ContextPropagator p : propagators) {
                    p.restore();
                }
                // 원본 작업 실행
                runnable.run();
            } finally {
                // 작업 완료 후, ThreadLocal을 반드시 정리하여 다른 작업에 영향을 주지 않도록 함
                for (ContextPropagator p : propagators) {
                    p.clear();
                }
            }
        };
    }

    public <T> Callable<T> wrap(Callable<T> callable) {
        return () -> {
            for (ContextPropagator p : propagators) {
                p.restore();
            }
            try {
                return callable.call();
            } finally {
                for (ContextPropagator p : propagators) {
                    p.clear();
                }
            }
        };
    }
}
