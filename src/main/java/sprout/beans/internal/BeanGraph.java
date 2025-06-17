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
                // 수정된 로직: 의존성 타입이 인터페이스인 경우, 해당 인터페이스를 구현하는 빈을 찾음
                boolean dependencyFoundInNodeMap = false;
                Class<?> actualDependencyType = null; // 실제로 nodeMap에 존재하는 구현체 타입

                if (nodeMap.containsKey(dep)) { // 직접적으로 dep 타입이 nodeMap에 있는 경우 (대부분 구체 클래스)
                    dependencyFoundInNodeMap = true;
                    actualDependencyType = dep;
                } else if (dep.isInterface() || Modifier.isAbstract(dep.getModifiers())) {
                    // 의존성 타입이 인터페이스 또는 추상 클래스인 경우
                    // nodeMap에 있는 BeanDefinition들 중에서 이 인터페이스/추상 클래스를 구현하는 (또는 상속하는) 빈을 찾음
                    for (Class<?> candidateType : nodeMap.keySet()) {
                        if (dep.isAssignableFrom(candidateType)) { // candidateType이 dep를 구현/상속한다면
                            // 이 경우, 인터페이스에 대한 의존성은 해당 인터페이스의 "구현체"에 대한 의존성이 됨
                            // 여러 구현체가 있을 수 있지만, 위상 정렬 목적에서는 어느 하나가 먼저 생성되면 되므로
                            // 일단 찾은 첫 번째 구현체를 실제 의존성으로 간주함

                            actualDependencyType = candidateType;
                            dependencyFoundInNodeMap = true;
                            break;
                        }
                    }
                }

                if (!dependencyFoundInNodeMap) {
                    // 컨테이너가 관리하는 빈 중 의존성(또는 그 구현체)을 찾지 못한 경우
                    // (외부 의존성이거나, 잘못된 의존성)
                    continue;
                }

                // 이제 actualDependencyType이 nodeMap에 있는 실제 의존성 빈의 타입
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