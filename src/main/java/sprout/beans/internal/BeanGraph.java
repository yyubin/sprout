package sprout.beans.internal;

import sprout.beans.BeanDefinition;

import java.util.*;

/**
 * Builds a dependency graph of BeanDefinitions and produces a topologically‑sorted
 * creation order.  Circular dependencies are detected and reported early.
 */
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

    /** Returns beans in a safe creation order (parents → children). */
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

    // ---------------------------------------------------------------------
    // internals
    // ---------------------------------------------------------------------

    private void buildEdges() {
        for (BeanDefinition def : nodeMap.values()) {
            for (Class<?> dep : def.dependencies()) {
                if (!nodeMap.containsKey(dep)) {
                    // external dependency (e.g. provided by container beforehand) – ignore
                    continue;
                }
                edges.computeIfAbsent(dep, k -> new ArrayList<>()).add(def.type());
                indegree.merge(def.type(), 1, Integer::sum);
            }
        }
    }

    // ---------------------------------------------------------------------
    // Exception type
    // ---------------------------------------------------------------------

    public static class CircularDependencyException extends RuntimeException {
        public CircularDependencyException(String message) {
            super(message);
        }
    }
}