package sprout.aop;

import sprout.aop.annotation.BeforeAuthCheck;
import app.domain.grade.MemberGrade;
import app.exception.NotLoggedInException;
import app.exception.UnauthorizedAccessException;
import app.message.ExceptionMessage;
import app.service.interfaces.MemberAuthServiceInterface;
import app.util.Session;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class MethodProxyHandler implements InvocationHandler {

    private final Object target;
    private final MemberAuthServiceInterface memberAuthService;

    public MethodProxyHandler(Object target, MemberAuthServiceInterface memberAuthService) {
        this.target = target;
        this.memberAuthService = memberAuthService;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (Session.getSessionId() == null) {
            throw new NotLoggedInException(ExceptionMessage.NOT_LOGGED_IN);
        }
        Method targetMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());
        if (targetMethod.isAnnotationPresent(BeforeAuthCheck.class)) {
            MemberGrade memberGrade = memberAuthService.checkAuthority(Session.getSessionId());
            if (memberGrade != MemberGrade.ADMIN) {
                throw new UnauthorizedAccessException(ExceptionMessage.UNAUTHORIZED_CREATE_BOARD);
            }
        }

        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    public static Object createProxy(Object target, MemberAuthServiceInterface memberAuthService) {
        return Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                target.getClass().getInterfaces(),
                new MethodProxyHandler(target, memberAuthService)
        );
    }
}
