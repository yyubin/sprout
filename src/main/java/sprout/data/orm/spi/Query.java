package sprout.data.orm.spi;

import java.util.List;

public interface Query {
    List getResultList();
    Object getSingleResult();
    int executeUpdate(); // 벌크 연산용 (UPDATE, DELETE)

    Query setParameter(String name, Object value);
    Query setParameter(int position, Object value);

    Query setFirstResult(int startPosition); // 페이징용
    Query setMaxResults(int maxResult); // 페이징용
    Query setFlushMode(FlushModeType flushMode);
}
