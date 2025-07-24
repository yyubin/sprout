package sprout.data.orm.spi;

import java.util.Map;

public interface AttributeNode<T> {
    public String getAttributeName();
    public Map<Class, Subgraph> getSubgraphs();
}
