package app.circular;
import sprout.beans.annotation.Component;

@Component
public class CircularDependencyB {
    public CircularDependencyB(CircularDependencyA a) {
    }
}