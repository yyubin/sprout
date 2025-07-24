package sprout.server;

import sprout.beans.BeanDefinition;
import sprout.beans.ConstructorBeanDefinition;
import sprout.beans.InfrastructureBean;
import sprout.beans.annotation.Bean;
import sprout.beans.annotation.Component;
import sprout.beans.processor.BeanDefinitionRegistrar;
import sprout.config.AppConfig;
import sprout.context.ContextPropagator;
import sprout.mvc.dispatcher.RequestDispatcher;
import sprout.mvc.http.parser.HttpRequestParser;
import sprout.server.builtins.BioHttpProtocolHandler;
import sprout.server.builtins.NioHttpProtocolHandler;
import sprout.server.builtins.RequestExecutorPoolService;
import sprout.server.builtins.VirtualRequestExecutorService;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// FIX : 사용 x, Registrar 순회 시점에 주입 불가
// ServerConfiguration 에서 처리
@Deprecated
public class ServerAutoConfigurationRegistrar implements BeanDefinitionRegistrar {

    private final AppConfig appConfig;

    public ServerAutoConfigurationRegistrar(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    public Collection<BeanDefinition> registerAdditionalBeanDefinitions(Collection<BeanDefinition> existingDefs) throws NoSuchMethodException {
        String executionMode = appConfig.getStringProperty("server.execution-mode", "hybrid");
        if (executionMode.equals("hybrid")) {
            return registerHybridServerBeans(existingDefs);
        }
        return registerNioServerBeans(existingDefs);
    }

    private Collection<BeanDefinition> registerHybridServerBeans(Collection<BeanDefinition> existingDefs) throws NoSuchMethodException {
        Constructor<?> constructor = BioHttpProtocolHandler.class.getConstructor(RequestDispatcher.class, HttpRequestParser.class, RequestExecutorService.class);
        return List.of(registerThreadTypeBean(existingDefs), createBeanDefinition("httpProtocolHandler", BioHttpProtocolHandler.class, constructor));
    }

    private Collection<BeanDefinition> registerNioServerBeans(Collection<BeanDefinition> existingDefs) throws NoSuchMethodException {
        Constructor<?> constructor = NioHttpProtocolHandler.class.getConstructor(RequestDispatcher.class, HttpRequestParser.class, RequestExecutorService.class);
        return List.of(registerThreadTypeBean(existingDefs), createBeanDefinition("httpProtocolHandler", NioHttpProtocolHandler.class, constructor));
    }

    private BeanDefinition registerThreadTypeBean(Collection<BeanDefinition> existingDefs) throws NoSuchMethodException {
        String threadType = appConfig.getStringProperty("server.thread-type", "virtual");
        if (threadType.equals("virtual")) {
            Constructor<?> constructor = VirtualRequestExecutorService.class.getConstructor(List.class);
            return createBeanDefinition("requestExecutorService", VirtualRequestExecutorService.class, constructor);
        } else {
            Constructor<?> constructor = RequestExecutorPoolService.class.getConstructor(int.class);
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            Object[] constructorArguments = new Object[]{appConfig.getIntProperty("server.thread-pool-size", 100)};
            return createBeanDefinition("requestExecutorService", RequestExecutorPoolService.class, constructor, parameterTypes, constructorArguments);
        }
    }

    private BeanDefinition createBeanDefinition(String beanName, Class<?> beanClass, Constructor<?> constructor) throws NoSuchMethodException {
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        return new ConstructorBeanDefinition(beanName, beanClass, constructor, parameterTypes);
    }

    private BeanDefinition createBeanDefinition(String beanName, Class<?> beanClass, Constructor<?> constructor, Class<?>[] parameterTypes, Object[] constructorArguments) {
        return new ConstructorBeanDefinition(beanName, beanClass, constructor, parameterTypes, constructorArguments);
    }
}
