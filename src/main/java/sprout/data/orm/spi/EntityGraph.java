package sprout.data.orm.spi;

import sprout.data.orm.spi.meta.Attribute;

import java.util.List;

// 어떤 엔티티의 어떤 연관관계를 EAGER하게 로딩할지 명시
public interface EntityGraph<T> {
    public String getName();
    public void addAttributeNodes(String ... attributeName);
    public void addAttributeNodes(Attribute<T, ?>... attribute);
    public List<AttributeNode<?>> getAttributeNodes();
    public <X> Subgraph<X> addSubgraph(Attribute<T, X> attribute);
    public <X> Subgraph<? extends X> addSubgraph(Attribute<T, X> attribute, Class<? extends X> type);
    public <X> Subgraph<X> addSubgraph(String attributeName);
    public <X> Subgraph<X> addSubgraph(String attributeName, Class<X> type);
    public <X> Subgraph<X> addKeySubgraph(Attribute<T, X> attribute);
    public <T> Subgraph<? extends T> addSubclassSubgraph(Class<? extends T> type);
    public Class<T> getClassType();
}
