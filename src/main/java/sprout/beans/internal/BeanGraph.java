package sprout.beans.internal;

import sprout.beans.BeanDefinition;

import java.lang.reflect.Modifier;
import java.util.*;


public class BeanGraph {
    private final Map<Class<?>, BeanDefinition> nodeMap = new HashMap<>();
    private final Map<Class<?>, List<Class<?>>> edges = new HashMap<>();
    private final Map<Class<?>, Integer> indegree = new HashMap<>();

    public BeanGraph(Collection<BeanDefinition> definitions) {
        definitions.forEach(d -> {
            nodeMap.put(d.type(), d);
            indegree.putIfAbsent(d.type(), 0);
        });
        buildEdges();
    }

    public List<BeanDefinition> topologicallySorted() {
        Deque<Class<?>> q = new ArrayDeque<>();
        indegree.forEach((cls, deg) -> {
            if (deg == 0) q.add(cls);
        });

        List<BeanDefinition> ordered = new ArrayList<>(nodeMap.size());
        while (!q.isEmpty()) {
            Class<?> cur = q.poll();
            ordered.add(nodeMap.get(cur));
            for (Class<?> next : edges.getOrDefault(cur, List.of())) {
                int d = indegree.merge(next, -1, Integer::sum);
                if (d == 0) q.add(next);
            }
        }

        if (ordered.size() != nodeMap.size()) {
            throw new CircularDependencyException("Circular dependency detected among application beans");
        }
        return ordered;
    }


    private void buildEdges() {
        for (BeanDefinition def : nodeMap.values()) {
            for (Class<?> dep : def.dependencies()) {
                boolean dependencyFoundInNodeMap = false;
                Class<?> actualDependencyType = null;

                if (nodeMap.containsKey(dep)) {
                    dependencyFoundInNodeMap = true;
                    actualDependencyType = dep;
                } else if (dep.isInterface() || Modifier.isAbstract(dep.getModifiers())) {
                    for (Class<?> candidateType : nodeMap.keySet()) {
                        if (dep.isAssignableFrom(candidateType)) {
                            actualDependencyType = candidateType;
                            dependencyFoundInNodeMap = true;
                            break;
                        }
                    }
                }

                if (!dependencyFoundInNodeMap) {
                    continue;
                }

                edges.computeIfAbsent(actualDependencyType, k -> new ArrayList<>()).add(def.type());
                indegree.merge(def.type(), 1, Integer::sum);
            }
        }
    }

    public static class CircularDependencyException extends RuntimeException {
        public CircularDependencyException(String message) {
            super(message);
        }
    }
}