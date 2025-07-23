package app.test.circular;
import sprout.beans.annotation.Component;

public class CircularDependencyB {
    public CircularDependencyB(CircularDependencyA a) {
    }
}