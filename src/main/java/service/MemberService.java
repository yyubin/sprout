package service;

import domain.Member;
import dto.MemberRegisterDTO;
import dto.MemberUpdateDTO;
import exception.MemberEmailAlreadyExistsException;
import exception.MemberIdAlreadyExistsException;
import exception.MemberNotFoundException;
import message.ExceptionMessage;
import repository.InMemoryMemberRepository;
import util.PasswordUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class MemberService {
    private final InMemoryMemberRepository memberRepository;

    public MemberService(InMemoryMemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public void registerMember(MemberRegisterDTO memberRegisterDTO) throws MemberIdAlreadyExistsException, MemberEmailAlreadyExistsException {
        checkIdExists(memberRegisterDTO.getId());
        checkEmailExists(memberRegisterDTO.getEmail());

        String encryptedPassword = PasswordUtil.encryptPassword(memberRegisterDTO.getPassword());
        Member newMember = new Member(
                memberRegisterDTO.getId(),
                memberRegisterDTO.getName(),
                memberRegisterDTO.getEmail(),
                LocalDate.now(),
                encryptedPassword
        );
        memberRepository.save(newMember);
    }

    public void registerAdminMember() {
        Member member = Member.makeAdminForTest();
        memberRepository.save(member);
    }

    public Optional<Member> getMemberById(String memberId) {
        return memberRepository.findById(memberId);
    }

    public Optional<Member> getMemberByEmail(String email) {
        return memberRepository.findByEmail(email);
    }

    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    public void updateMember(String memberId, MemberUpdateDTO memberUpdateDTO) throws MemberNotFoundException{
        checkIdDoesNotExists(memberId);
        if (memberUpdateDTO.getPassword() != null) {
            memberUpdateDTO.setPassword(PasswordUtil.encryptPassword(memberUpdateDTO.getPassword()));
        }
        memberRepository.update(memberId, memberUpdateDTO);
    }

    public void deleteMember(String memberId) throws MemberNotFoundException {
        checkIdDoesNotExists(memberId);
        memberRepository.delete(memberId);
    }

    private void checkIdExists(String memberId) throws MemberIdAlreadyExistsException {
        if (getMemberById(memberId).isPresent()) {
            throw new MemberIdAlreadyExistsException(ExceptionMessage.MEMBER_ID_ALREADY_EXISTS);
        }
    }

    private void checkEmailExists(String email) throws MemberEmailAlreadyExistsException {
        if (getMemberByEmail(email).isPresent()) {
            throw new MemberEmailAlreadyExistsException(ExceptionMessage.MEMBER_EMAIL_ALREADY_EXISTS);
        }
    }

    private void checkIdDoesNotExists(String memberId) throws MemberNotFoundException {
        if (getMemberById(memberId).isEmpty()) {
            throw new MemberNotFoundException(ExceptionMessage.MEMBER_NOT_FOUND);
        }
    }

}
