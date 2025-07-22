package sprout.aop.advisor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DefaultPointcutFactoryTest {

    DefaultPointcutFactory f = new DefaultPointcutFactory();

    @interface A1 {}
    @interface A2 {}

    @Test
    void onlyAnno_returnsAnnotationPointcut() {
        Pointcut pc = f.createPointcut(new Class[]{A1.class}, "");
        assertTrue(pc instanceof AnnotationPointcut);
    }

    @Test
    void onlyAspectJ_returnsAspectJAdapter() {
        Pointcut pc = f.createPointcut(new Class[]{}, "execution(* foo..*(..))");
        assertTrue(pc instanceof AspectJPointcutAdapter);
    }

    @Test
    void both_returnsComposite() {
        Pointcut pc = f.createPointcut(new Class[]{A1.class}, "execution(* foo..*(..))");
        assertTrue(pc instanceof CompositePointcut);
    }

    @Test
    void none_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> f.createPointcut(new Class[]{}, ""));
    }
}
