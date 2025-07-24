package sprout.data.orm.spi;

public enum LockModeType {
//    낙관적 락
//    실제 DB 락 안 걸림
//    엔티티 버전(version 필드) 기반으로 충돌 감지
//    다른 트랜잭션이 동시에 수정해도 일단 허용 → 커밋 시점에 버전이 바뀌었는지 확인
//    READ는 OPTIMISTIC이랑 동의어지만, 새 코드에는 OPTIMISTIC 쓰는 게 좋을 듯? READ 안넣음
    OPTIMISTIC,
    READ,

    // 낙관적 락
    // 근데 커밋 안해도 버전 증가 시킴
    // 병렬 업데이트 시, 수정했다고 표기하고 싶을 때 사용
    // 조회만 해도 버전 올라가니까, 다른 트랜잭션이 못 만짐
    // WRITE 이랑 동의어
    OPTIMISTIC_FORCE_INCREMENT,
    WRITE,

    // 비관적 락 → 실제 DB 락
    // 다른 트랜잭션이 이 데이터를 수정 못 하게 읽기 락
    // 근데 읽기 락이라서 다른 사람도 읽는 건 됨. 수정만 못 함
    PESSIMISTIC_READ,

    // 가장 강한 락
    // 수정하려는 데이터를 다른 트랜잭션이 절대 접근 못 하게 락 걸기
    PESSIMISTIC_WRITE,

    // PESSIMISTIC_WRITE랑 거의 같지만 버전도 증가 시킴
    PESSIMISTIC_FORCE_INCREMENT,

    // 아무 락 없음
    NONE
}
