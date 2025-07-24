package sprout.beans.internal;

import sprout.beans.BeanCreationMethod;
import sprout.beans.BeanDefinition;

import java.lang.reflect.Modifier;
import java.util.*;


public class BeanGraph {
    private final Map<String, BeanDefinition> nodeMap = new HashMap<>();
    private final Map<String, List<String>> edges = new HashMap<>();
    private final Map<String, Integer> indegree = new HashMap<>();

    public BeanGraph(Collection<BeanDefinition> definitions) {
        definitions.forEach(d -> {
            // BeanDefinition의 getName()을 사용하여 빈 이름을 키로 사용
            nodeMap.put(d.getName(), d);
            // 초기 indegree는 0으로 설정
            indegree.putIfAbsent(d.getName(), 0);
        });
        buildEdges();
        System.out.println(indegree);
    }

    public List<BeanDefinition> topologicallySorted() {
        Deque<String> q = new ArrayDeque<>();
        indegree.forEach((beanName, deg) -> {
            if (deg == 0) q.add(beanName);
        });

        List<BeanDefinition> ordered = new ArrayList<>(nodeMap.size());
        while (!q.isEmpty()) {
            String curBeanName = q.poll();
            ordered.add(nodeMap.get(curBeanName));

            // 현재 빈이 의존하는 다른 빈들 (curBeanName -> nextBeanName)의 indegree 감소
            for (String nextBeanName : edges.getOrDefault(curBeanName, Collections.emptyList())) {
                int d = indegree.merge(nextBeanName, -1, Integer::sum);
                if (d == 0) q.add(nextBeanName); // indegree가 0이 되면 큐에 추가
            }
        }

        // 그래프에 모든 노드가 포함되지 않았다면 순환 의존성 발생
        if (ordered.size() != nodeMap.size()) {
            throw new CircularDependencyException("Circular dependency detected among application beans");
        }
        return ordered;
    }


    private void buildEdges() {
        for (BeanDefinition def : nodeMap.values()) {
            Class<?>[] dependencyTypes = null;

            if (def.getCreationMethod() == BeanCreationMethod.CONSTRUCTOR) {
                dependencyTypes = def.getConstructorArgumentTypes();
            } else if (def.getCreationMethod() == BeanCreationMethod.FACTORY_METHOD) {
                dependencyTypes = def.getFactoryMethodArgumentTypes();
                String factoryBeanName = def.getFactoryBeanName();
                if (factoryBeanName != null && nodeMap.containsKey(factoryBeanName)) {
                    edges.computeIfAbsent(factoryBeanName, k -> new ArrayList<>()).add(def.getName());
                    indegree.merge(def.getName(), 1, Integer::sum);
                } else if (factoryBeanName != null) {
                    System.err.println("Warning: Factory bean '" + factoryBeanName + "' for '" + def.getName() + "' not found in container definitions.");
                }
            } else {
                continue;
            }

            if (dependencyTypes == null || dependencyTypes.length == 0) {
                continue;
            }

            for (Class<?> depType : dependencyTypes) {
                if (List.class.isAssignableFrom(depType)) {
                    continue;
                }

                // 해당 타입에 맞는 빈 이름을 모두 찾기
                Set<String> candidateBeanNames = getBeanNamesForType(depType);
                String actualDependencyBeanName = null;
                if (candidateBeanNames.size() == 1) {
                    // 정확히 하나의 후보가 있다면 그 빈이 의존성
                    actualDependencyBeanName = candidateBeanNames.iterator().next();
                } else if (candidateBeanNames.size() > 1) {
                    throw new RuntimeException(
                            "Ambiguous dependency for type '" + depType.getName() + "' required by bean '" + def.getName() + "'. " +
                                    "Found multiple candidates: " + candidateBeanNames + ". "
                    );
                } else {
                    // 해당 타입의 빈을 컨테이너 정의에서 찾을 수 없는 경우
                    // (Spring은 이런 경우를 'optional'로 처리하거나 런타임에 MissingBeanException 발생)
                    continue;
                }

                if (actualDependencyBeanName != null) {
                    edges.computeIfAbsent(actualDependencyBeanName, k -> new ArrayList<>()).add(def.getName());
                    indegree.merge(def.getName(), 1, Integer::sum);
                }
            }
        }
    }

    private Set<String> getBeanNamesForType(Class<?> type) {
        Set<String> names = new HashSet<>();
        for (BeanDefinition beanDef : nodeMap.values()) {
            if (type.isAssignableFrom(beanDef.getType())) {
                names.add(beanDef.getName());
            }
        }
        return names;
    }

    public static class CircularDependencyException extends RuntimeException {
        public CircularDependencyException(String message) {
            super(message);
        }
    }
}