package sprout.data.orm.engine;

import sprout.data.orm.action.Executable;

import java.io.Serializable;

public interface ComparableExecutable extends Executable, Comparable<Executable>, Serializable {
    String getPrimarySort();
    String getSecondarySort();
}
