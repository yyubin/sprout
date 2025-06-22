package sprout.mvc.argument;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TypeConverterTest {

    @Test
    @DisplayName("생성자가 호출되지 않도록 private으로 선언되었는지 확인")
    void constructorIsPrivate() {
        // 리플렉션을 사용하여 private 생성자에 접근하여 인스턴스 생성을 시도
        // 이는 유틸리티 클래스가 인스턴스화되지 않도록 보장하는 테스트
        assertThrows(IllegalAccessException.class, () ->
                TypeConverter.class.getDeclaredConstructor().newInstance()
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"hello", "123", "", "true", "false"})
    @DisplayName("String 타입으로 변환 시 입력된 String 값을 그대로 반환해야 한다")
    void convert_ToString_ReturnsOriginalString(String value) {
        Object converted = TypeConverter.convert(value, String.class);
        assertThat(converted).isEqualTo(value);
        assertThat(converted).isInstanceOf(String.class);
    }

    @ParameterizedTest
    @CsvSource({
            "123, 123",
            "0, 0",
            "-456, -456",
            "9223372036854775807, 9223372036854775807" // Long.MAX_VALUE
    })
    @DisplayName("Long과 long 타입으로 변환 시 올바른 Long 값을 반환해야 한다")
    void convert_ToLong_ReturnsCorrectLong(String value, long expected) {
        Object convertedLongObj = TypeConverter.convert(value, Long.class);
        assertThat(convertedLongObj).isEqualTo(expected);
        assertThat(convertedLongObj).isInstanceOf(Long.class);

        Object convertedLongPri = TypeConverter.convert(value, long.class);
        assertThat(convertedLongPri).isEqualTo(expected);
        assertThat(convertedLongPri).isInstanceOf(Long.class); // primitive long도 Long wrapper로 박싱됨
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "123L", "1.23"})
    @DisplayName("Long으로 변환 불가능한 문자열인 경우 NumberFormatException을 던져야 한다")
    void convert_ToLong_ThrowsNumberFormatExceptionForInvalidString(String value) {
        assertThrows(NumberFormatException.class, () ->
                TypeConverter.convert(value, Long.class)
        );
        assertThrows(NumberFormatException.class, () ->
                TypeConverter.convert(value, long.class)
        );
    }

    @ParameterizedTest
    @CsvSource({
            "123, 123",
            "0, 0",
            "-456, -456",
            "2147483647, 2147483647" // Integer.MAX_VALUE
    })
    @DisplayName("Integer와 int 타입으로 변환 시 올바른 Integer 값을 반환해야 한다")
    void convert_ToInteger_ReturnsCorrectInteger(String value, int expected) {
        Object convertedIntObj = TypeConverter.convert(value, Integer.class);
        assertThat(convertedIntObj).isEqualTo(expected);
        assertThat(convertedIntObj).isInstanceOf(Integer.class);

        Object convertedIntPri = TypeConverter.convert(value, int.class);
        assertThat(convertedIntPri).isEqualTo(expected);
        assertThat(convertedIntPri).isInstanceOf(Integer.class); // primitive int도 Integer wrapper로 박싱됨
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "123.45", "123L"})
    @DisplayName("Integer로 변환 불가능한 문자열인 경우 NumberFormatException을 던져야 한다")
    void convert_ToInteger_ThrowsNumberFormatExceptionForInvalidString(String value) {
        assertThrows(NumberFormatException.class, () ->
                TypeConverter.convert(value, Integer.class)
        );
        assertThrows(NumberFormatException.class, () ->
                TypeConverter.convert(value, int.class)
        );
    }

    @ParameterizedTest
    @CsvSource({
            "true, true",
            "false, false",
            "TRUE, true",
            "FALSE, false",
            "TrUe, true",
            "fAlSe, false",
            "anyOtherString, false" // Boolean.parseBoolean은 'true' 문자열만 true로 변환
    })
    @DisplayName("Boolean과 boolean 타입으로 변환 시 올바른 Boolean 값을 반환해야 한다")
    void convert_ToBoolean_ReturnsCorrectBoolean(String value, boolean expected) {
        Object convertedBoolObj = TypeConverter.convert(value, Boolean.class);
        assertThat(convertedBoolObj).isEqualTo(expected);
        assertThat(convertedBoolObj).isInstanceOf(Boolean.class);

        Object convertedBoolPri = TypeConverter.convert(value, boolean.class);
        assertThat(convertedBoolPri).isEqualTo(expected);
        assertThat(convertedBoolPri).isInstanceOf(Boolean.class); // primitive boolean도 Boolean wrapper로 박싱됨
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("null 문자열을 String, Long, Integer, Boolean 래퍼 타입으로 변환 시 null을 반환해야 한다")
    void convert_NullValue_ToWrapperTypes_ReturnsNull(String value) {
        assertThat(TypeConverter.convert(value, String.class)).isNull();
        assertThat(TypeConverter.convert(value, Long.class)).isNull();
        assertThat(TypeConverter.convert(value, Integer.class)).isNull();
        assertThat(TypeConverter.convert(value, Boolean.class)).isNull();
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("null 문자열을 primitive 타입으로 변환 시 IllegalArgumentException을 던져야 한다")
    void convert_NullValue_ToPrimitiveTypes_ThrowsIllegalArgumentException(String value) {
        assertThrows(IllegalArgumentException.class, () ->
                TypeConverter.convert(value, long.class)
        );
        assertThrows(IllegalArgumentException.class, () ->
                TypeConverter.convert(value, int.class)
        );
        assertThrows(IllegalArgumentException.class, () ->
                TypeConverter.convert(value, boolean.class)
        );
    }


    @Test
    @DisplayName("지원하지 않는 타입으로 변환 시 IllegalArgumentException을 던져야 한다")
    void convert_ToUnsupportedType_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                TypeConverter.convert("123.45", Double.class) // 현재 Double은 지원하지 않음
        );
        assertThrows(IllegalArgumentException.class, () ->
                TypeConverter.convert("a", Character.class) // 현재 Character는 지원하지 않음
        );
        assertThrows(IllegalArgumentException.class, () ->
                TypeConverter.convert("2023-01-01", java.time.LocalDate.class) // 현재 LocalDate는 지원하지 않음
        );
    }
}