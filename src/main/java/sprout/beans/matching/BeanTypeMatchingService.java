package sprout.beans.matching;

import sprout.beans.BeanDefinition;

import java.util.*;

/**
 * 빈 타입 매칭 서비스
 * 타입을 기반으로 빈을 찾고 매칭하는 로직을 담당
 */
public class BeanTypeMatchingService {

    private final Map<String, BeanDefinition> beanDefinitions;
    private final Map<String, Object> singletons;

    public BeanTypeMatchingService(Map<String, BeanDefinition> beanDefinitions, Map<String, Object> singletons) {
        this.beanDefinitions = beanDefinitions;
        this.singletons = singletons;
    }

    // 주어진 타입에 대한 후보 빈 이름들을 찾습니다
    public Set<String> findCandidateNamesForType(Class<?> type) {
        Set<String> names = new HashSet<>();

        // 1) 이미 등록된 싱글턴에서 찾기
        for (Map.Entry<String, Object> entry : singletons.entrySet()) {
            if (type.isAssignableFrom(entry.getValue().getClass())) {
                names.add(entry.getKey());
            }
        }

        // 2) 아직 생성되지 않은 BeanDefinition에서 찾기
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitions.entrySet()) {
            if (type.isAssignableFrom(entry.getValue().getType())) {
                names.add(entry.getKey());
            }
        }

        return names;
    }

    /**
     * 후보 빈들 중에서 primary 빈을 선택
     *
     * @param requiredType 요청된 타입
     * @param candidates 후보 빈 이름들
     * @param primaryTypeToNameMap 타입별 primary 빈 매핑
     * @return primary 빈 이름 (없으면 null)
     */
    public String choosePrimary(Class<?> requiredType, Set<String> candidates, Map<Class<?>, String> primaryTypeToNameMap) {
        // 1) BeanDefinition 중 primary=true인 빈 찾기
        List<String> primaries = candidates.stream()
                .filter(name -> {
                    BeanDefinition def = beanDefinitions.get(name);
                    return def != null && def.isPrimary();
                })
                .toList();

        if (primaries.size() == 1) {
            return primaries.get(0);
        }
        if (primaries.size() > 1) {
            throw new RuntimeException("@Primary beans conflict for type " + requiredType.getName() + ": " + primaries);
        }

        // 2) primaryTypeToNameMap fallback
        String mapped = primaryTypeToNameMap.get(requiredType);
        if (mapped != null && candidates.contains(mapped)) {
            return mapped;
        }

        return null;
    }

    /**
     * 주어진 타입의 모든 빈 이름을 BeanDefinition에서 찾기
     * (BeanGraph에서 사용)
     *
     * @param type 찾을 타입
     * @return 빈 이름 집합
     */
    public Set<String> getBeanNamesForType(Class<?> type) {
        Set<String> names = new HashSet<>();
        for (BeanDefinition beanDef : beanDefinitions.values()) {
            if (type.isAssignableFrom(beanDef.getType())) {
                names.add(beanDef.getName());
            }
        }
        return names;
    }
}
