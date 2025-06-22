package sprout.mvc.argument;

public final class TypeConverter {
    private TypeConverter() {}

    public static Object convert(String value, Class<?> targetType) {
        if (value == null) {
            if (targetType.isPrimitive()) {
                throw new IllegalArgumentException("Null value cannot be assigned to primitive type: " + targetType.getName());
            }
            return null;
        }

        if (targetType.equals(String.class)) {
            return value;
        } else if (targetType.equals(Long.class) || targetType.equals(long.class)) {
            return Long.parseLong(value);
        } else if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
            return Integer.parseInt(value);
        } else if (targetType.equals(Boolean.class) || targetType.equals(boolean.class)) {
            return Boolean.parseBoolean(value);
        }
        throw new IllegalArgumentException("Cannot convert String value [" + value + "] to target class [" + targetType.getName() + "]");
    }
}
