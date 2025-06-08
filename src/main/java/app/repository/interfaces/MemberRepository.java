package app.repository.interfaces;

import app.domain.Member;
import app.dto.MemberUpdateDTO;

import java.util.List;
import java.util.Optional;

public interface MemberRepository {

    void save(Member member);
    Optional<Member> findById(String id);
    Optional<Member> findByEmail(String email);
    List<Member> findAll();
    void update(String memberId, MemberUpdateDTO updatedMemberInfo);
    void delete(String memberId);

}
