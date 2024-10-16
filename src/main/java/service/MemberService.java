package service;

import domain.Member;
import dto.MemberRegisterDTO;
import dto.MemberUpdateDTO;
import repository.InMemoryMemberRepository;

import java.util.List;
import java.util.Optional;

public class MemberService {
    private final InMemoryMemberRepository memberRepository = new InMemoryMemberRepository();

    public void registerMember(MemberRegisterDTO memberRegisterDTO) {
        memberRepository.save(memberRegisterDTO);
    }

    public Optional<Member> getMemberById(String memberId) {
        return memberRepository.findById(memberId);
    }

    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    public void updateMember(String memberId, MemberUpdateDTO memberUpdateDTO) {
        memberRepository.update(memberId, memberUpdateDTO);
    }

    public void deleteMember(String memberId) {
        memberRepository.delete(memberId);
    }

}
