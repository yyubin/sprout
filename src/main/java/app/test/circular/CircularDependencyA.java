package app.test.circular;

import sprout.beans.annotation.Component;

public class CircularDependencyA {
    public CircularDependencyA(CircularDependencyB b) {
    }
}