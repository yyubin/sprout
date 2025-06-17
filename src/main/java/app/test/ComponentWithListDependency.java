package app.test;
import sprout.beans.annotation.Component;
import java.util.List;

@Component
public class ComponentWithListDependency {
    private final List<SomeService> services; // SomeService 구현체들이 주입될 예정

    public ComponentWithListDependency(List<SomeService> services) {
        this.services = services;
    }

    public List<SomeService> getServices() {
        return services;
    }

    public int getServiceCount() {
        return services.size();
    }
}