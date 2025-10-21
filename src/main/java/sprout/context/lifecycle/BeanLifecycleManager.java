package sprout.context.lifecycle;

import java.util.Comparator;
import java.util.List;

/**
 * 빈 생명주기를 관리하는 매니저
 * 등록된 Phase들을 순서대로 실행
 */
public class BeanLifecycleManager {

    private final List<BeanLifecyclePhase> phases;

    public BeanLifecycleManager(List<BeanLifecyclePhase> phases) {
        this.phases = phases.stream()
                .sorted(Comparator.comparingInt(BeanLifecyclePhase::getOrder))
                .toList();
    }

    public void executePhases(BeanLifecyclePhase.PhaseContext context) throws Exception {
        for (BeanLifecyclePhase phase : phases) {
            System.out.println("--- Executing Phase: " + phase.getName() + " (order=" + phase.getOrder() + ") ---");
            phase.execute(context);
        }
    }
}
