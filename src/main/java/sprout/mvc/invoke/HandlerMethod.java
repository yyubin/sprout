package sprout.mvc.invoke;

import sprout.mvc.mapping.ControllerInterface;

import java.lang.reflect.Method;

/**
 * Simple value‑object holding the target controller and its Method.
 */
public record HandlerMethod(ControllerInterface controller, Method method) { }