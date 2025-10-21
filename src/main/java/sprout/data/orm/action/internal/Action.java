package sprout.data.orm.action.internal;

import sprout.data.orm.action.Executable;
import sprout.data.orm.engine.AfterTransactionCompletionProcess;
import sprout.data.orm.engine.ComparableExecutable;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class Action implements ComparableExecutable, AfterTransactionCompletionProcess {
    public enum Type { INSERT, UPDATE, DELETE }
    public abstract Type getActionType();
    abstract void execute(Connection connection) throws SQLException;

    @Override
    public int compareTo(Executable other) {
        if (!(other instanceof Action)) {
            return -1; // 혹은 예외 처리
        }
        Action otherAction = (Action) other;

        // 1. Action Type으로 비교 (INSERT < UPDATE < DELETE)
        int typeCompare = this.getActionType().compareTo(otherAction.getActionType());
        if (typeCompare != 0) {
            return typeCompare;
        }

        // 2. 같은 Type 내에서는 테이블(엔티티) 이름으로 비교
        //    (의존성 기반의 더 복잡한 정렬이 필요하지만, 우선은 이름으로)
        int primarySortCompare = this.getPrimarySort().compareTo(otherAction.getPrimarySort());
        if (primarySortCompare != 0) {
            // INSERT는 의존성 순, DELETE는 역순 정렬이 필요함
            if (this.getActionType() == Type.DELETE) {
                return -primarySortCompare; // 역순
            }
            return primarySortCompare;
        }

        // 3. 같은 테이블 내에서는 PK로 비교
        return this.getSecondarySort().compareTo(otherAction.getSecondarySort());
    }
}
