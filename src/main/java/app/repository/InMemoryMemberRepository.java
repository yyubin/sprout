package app.repository;

import sprout.beans.annotation.Repository;
import app.domain.Member;
import app.dto.MemberUpdateDTO;
import app.repository.interfaces.MemberRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class InMemoryMemberRepository implements MemberRepository {
    private List<Member> members = new ArrayList<>();

    public void save(Member member) {
        members.add(member);
    }

    public Optional<Member> findById(String id) {
        return members.stream()
                .filter(member -> member.getId().equals(id) && !member.isDeleted())
                .findFirst();
    }

    public Optional<Member> findByEmail(String email) {
        return members.stream()
                .filter(member -> member.getEmail().equals(email) && !member.isDeleted())
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


