package app.circular;

import sprout.beans.annotation.Component;

@Component
public class CircularDependencyA {
    public CircularDependencyA(CircularDependencyB b) {
    }
}