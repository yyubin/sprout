package sprout.mvc.invoke;

import sprout.mvc.mapping.ControllerInterface;
import sprout.mvc.mapping.RequestMappingInfo;

import java.lang.reflect.Method;

public record HandlerMethod(RequestMappingInfo requestMappingInfo) { }