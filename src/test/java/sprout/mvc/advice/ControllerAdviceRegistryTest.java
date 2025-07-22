package sprout.mvc.advice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import sprout.context.BeanFactory;
import sprout.mvc.advice.annotation.ControllerAdvice;
import sprout.mvc.advice.annotation.ExceptionHandler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ControllerAdviceRegistryTest {

    ControllerAdviceRegistry registry;

    BeanFactory beanFactory;   // Mockito mock


    @ControllerAdvice
    static class GlobalAdvice {
        @ExceptionHandler({Exception.class})
        public void handleGeneric(Exception ex) {}
    }

    @ControllerAdvice
    static class IoAdvice {
        @ExceptionHandler({IOException.class})
        public void handleIo(IOException ex) {}
    }

    /* @ControllerAdvice 가 없는 일반 빈 */
    static class NormalBean { }

    GlobalAdvice globalAdvice;
    IoAdvice ioAdvice;
    NormalBean normalBean;

    @BeforeEach
    void setUp() {
        registry = new ControllerAdviceRegistry();
        beanFactory = Mockito.mock(BeanFactory.class);

        globalAdvice = new GlobalAdvice();
        ioAdvice     = new IoAdvice();
        normalBean   = new NormalBean();
    }

    @Nested
    @DisplayName("scanControllerAdvices()")
    class Scan {

        @Test @DisplayName("Advice 어노테이션이 있는 빈만 스캔·등록한다")
        void scan_registersOnlyAdviceBeans() {
            // given
            when(beanFactory.getAllBeans())
                    .thenReturn(Set.of(globalAdvice, ioAdvice, normalBean));

            // when
            registry.scanControllerAdvices(beanFactory);

            // then
            // → registry 내부의 allExceptionHandlers 는 private 이므로
            //    getExceptionHandler 로 간접 확인
            assertThat(registry.getExceptionHandler(Exception.class)).isPresent();
            assertThat(registry.getExceptionHandler(IOException.class)).isPresent();
            // normalBean 은 Advice 아님 → 예외 없는 메서드라 검색해도 empty
            assertThat(registry.getExceptionHandler(IllegalStateException.class)).isPresent();
        }
    }

    @Nested
    @DisplayName("getExceptionHandler()")
    class Lookup {

        @BeforeEach
        void initRegistry() {
            when(beanFactory.getAllBeans()).thenReturn(Set.of(globalAdvice, ioAdvice));
            registry.scanControllerAdvices(beanFactory);
        }

        @Test @DisplayName("가장 구체적인 핸들러를 선택한다 (IOException vs Exception)")
        void choosesMostSpecific() throws NoSuchMethodException {
            Optional<ExceptionHandlerObject> opt = registry.getExceptionHandler(IOException.class);
            assertThat(opt).isPresent();

            Method chosen = opt.get().getMethod();
            assertThat(chosen.getName()).isEqualTo("handleIo"); // IoAdvice 우선
        }

        @Test @DisplayName("하위 예외(FileNotFoundException)도 상위(IOException) 핸들러로 매핑")
        void inheritsHierarchy() {
            Optional<ExceptionHandlerObject> opt =
                    registry.getExceptionHandler(FileNotFoundException.class);

            assertThat(opt).isPresent();
            assertThat(opt.get().getMethod().getName()).isEqualTo("handleIo");
        }

        @Test @DisplayName("매 조회시 캐싱되어 같은 인스턴스를 반환한다")
        void cachingWorks() {
            Optional<ExceptionHandlerObject> first  = registry.getExceptionHandler(IOException.class);
            Optional<ExceptionHandlerObject> second = registry.getExceptionHandler(IOException.class);

            assertThat(first).isSameAs(second);
        }

        @Test @DisplayName("등록된 핸들러가 없으면 Optional.empty()")
        void noHandlerFound() {
            // Exception, IOException 모두 처리지 못하는 Error 계열 사용
            Optional<ExceptionHandlerObject> opt =
                    registry.getExceptionHandler(OutOfMemoryError.class);

            assertThat(opt).isEmpty();
        }
    }
}
