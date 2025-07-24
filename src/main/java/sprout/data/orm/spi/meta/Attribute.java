package sprout.data.orm.spi.meta;

public interface Attribute<X, Y> {
    public static enum PersistentAttributeType {
        MANY_TO_ONE,
        ONE_TO_ONE,
        BASIC,
        EMBEDDED,
        MANY_TO_MANY,
        ONE_TO_MANY,
        ELEMENT_COLLECTION
    }
    String getName();
    PersistentAttributeType getPersistentAttributeType();
    Class<X> getDeclaringType();
    Class<Y> getJavaType();
    boolean isAssociation();
    boolean isCollection();
}
