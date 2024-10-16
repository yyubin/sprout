package repository;

import domain.Member;
import dto.MemberRegisterDTO;
import dto.MemberUpdateDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryMemberRepository {
    private List<Member> members = new ArrayList<>();

    public void save(MemberRegisterDTO memberRegisterDTO) {
        Member member = new Member(
                memberRegisterDTO.getId(),
                memberRegisterDTO.getName(),
                memberRegisterDTO.getEmail(),
                memberRegisterDTO.getJoinDate(),
                memberRegisterDTO.getPassword()
        );
        members.add(member);
    }

    public Optional<Member> findById(String id) {
        return members.stream()
                .filter(member -> member.getId().equals(id) && !member.isDeleted())
                .findFirst();
    }

    public List<Member> findAll() {
        return members.stream()
                .filter(member -> !member.isDeleted())
                .toList();
    }

    public void update(String memberId, MemberUpdateDTO updatedMemberInfo) {
        findById(memberId).ifPresent(member -> {
            if (updatedMemberInfo.getEmail() != null) {
                member.setEmail(updatedMemberInfo.getEmail());
            }
            if (updatedMemberInfo.getPassword() != null) {
                member.setEncryptedPassword(updatedMemberInfo.getPassword());
            }
        });
    }

    public void delete(String memberId) {
        findById(memberId).ifPresent(member -> {member.setDeleted(true);});
    }

}

