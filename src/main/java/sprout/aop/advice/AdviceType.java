package sprout.aop.advice;

import sprout.aop.annotation.After;
import sprout.aop.annotation.Around;
import sprout.aop.annotation.Before;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

public enum AdviceType {
    AROUND(Around.class),
    BEFORE(Before.class),
    AFTER(After.class);

    private final Class<? extends Annotation> anno;
    AdviceType(Class<? extends Annotation> anno) { this.anno = anno; }

    public static Optional<AdviceType> from(Method m) {
        return Arrays.stream(values())
                .filter(t -> m.isAnnotationPresent(t.anno))
                .findFirst();
    }

    public Class<? extends Annotation> anno() { return anno; }
}
