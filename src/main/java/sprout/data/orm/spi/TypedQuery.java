package sprout.data.orm.spi;

import java.util.List;

public interface TypedQuery<X> extends Query{
    List<X> getResultList();
    X getSingleResult();
    TypedQuery<X> setFirstResult(int startPosition);
    TypedQuery<X> setMaxResults(int maxResult);
    TypedQuery<X> setFlushMode(FlushModeType flushMode);

    TypedQuery<X> setParameter(String name, Object value);
    TypedQuery<X> setParameter(int position, Object value);

}
