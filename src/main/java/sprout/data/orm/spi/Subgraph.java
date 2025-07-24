package sprout.data.orm.spi;

import sprout.data.orm.spi.meta.Attribute;

import java.util.List;

public interface Subgraph<T>{
    public void addAttributeNodes(String ... attributeName);
    public void addAttributeNodes(Attribute<T, ?>... attribute);
    public <X> Subgraph<X> addSubgraph(Attribute<T, X> attribute);
    public <X> Subgraph<? extends X> addSubgraph(Attribute<T, X> attribute, Class<? extends X> type);
    public <X> Subgraph<X> addSubgraph(String attributeName);
    public <X> Subgraph<X> addSubgraph(String attributeName, Class<X> type);
    public List<AttributeNode<?>> getAttributeNodes();
    public Class<T> getClassType();
}
