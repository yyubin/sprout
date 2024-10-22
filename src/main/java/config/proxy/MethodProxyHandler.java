package config.proxy;

import config.annotations.BeforeAuthCheck;
import domain.grade.MemberGrade;
import exception.UnauthorizedAccessException;
import message.ExceptionMessage;
import service.interfaces.MemberAuthServiceInterface;
import util.Session;

import java.lang.reflect.InvocationHandler;
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
        if (method.isAnnotationPresent(BeforeAuthCheck.class)) {
            MemberGrade memberGrade = memberAuthService.checkAuthority(Session.getSessionId());
            if (memberGrade != MemberGrade.ADMIN) {
                throw new UnauthorizedAccessException(ExceptionMessage.UNAUTHORIZED_CREATE_BOARD);
            }
        }
        return method.invoke(target, args);
    }

    public static Object createProxy(Object target, MemberAuthServiceInterface memberAuthService) {
        return Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                target.getClass().getInterfaces(),
                new MethodProxyHandler(target, memberAuthService)
        );
    }
}