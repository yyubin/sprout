package service;

import config.annotations.Priority;
import config.annotations.Requires;
import config.annotations.Service;
import domain.Member;
import dto.MemberRegisterDTO;
import dto.MemberUpdateDTO;
import exception.MemberEmailAlreadyExistsException;
import exception.MemberIdAlreadyExistsException;
import exception.MemberNotFoundException;
import message.ExceptionMessage;
import repository.InMemoryMemberRepository;
import repository.interfaces.MemberRepository;
import service.interfaces.MemberServiceInterface;
import util.BCryptPasswordUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
@Priority(value = 0)
@Requires(dependsOn = {MemberRepository.class})
public class MemberService implements MemberServiceInterface {
    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public void registerMember(MemberRegisterDTO memberRegisterDTO)
            throws MemberIdAlreadyExistsException, MemberEmailAlreadyExistsException {
        checkIdExists(memberRegisterDTO.getId());
        checkEmailExists(memberRegisterDTO.getEmail());

        String encryptedPassword = BCryptPasswordUtil.encryptPassword(memberRegisterDTO.getPassword());
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

    public void updateMember(String memberId, MemberUpdateDTO memberUpdateDTO)
            throws MemberNotFoundException {
        checkIdDoesNotExists(memberId);
        if (memberUpdateDTO.getPassword() != null) {
            memberUpdateDTO.setPassword(BCryptPasswordUtil.encryptPassword(memberUpdateDTO.getPassword()));
        }
        memberRepository.update(memberId, memberUpdateDTO);
    }

    public void deleteMember(String memberId) throws MemberNotFoundException {
        checkIdDoesNotExists(memberId);
        memberRepository.delete(memberId);
    }

    private void checkIdExists(String memberId) throws MemberIdAlreadyExistsException {
        checkExists(memberId,
                () -> getMemberById(memberId).isPresent(),
                MemberIdAlreadyExistsException::new,
                ExceptionMessage.MEMBER_ID_ALREADY_EXISTS);
    }

    private void checkEmailExists(String email) throws MemberEmailAlreadyExistsException {
        checkExists(email,
                () -> getMemberByEmail(email).isPresent(),
                MemberEmailAlreadyExistsException::new,
                ExceptionMessage.MEMBER_EMAIL_ALREADY_EXISTS);
    }

    private void checkIdDoesNotExists(String memberId) throws MemberNotFoundException {
        checkExists(memberId,
                () -> getMemberById(memberId).isEmpty(),
                MemberNotFoundException::new,
                ExceptionMessage.MEMBER_NOT_FOUND);
    }

    private <T extends Throwable> void checkExists(String identifier,
                                                   Supplier<Boolean> checkFunction,
                                                   Function<String, T> exceptionSupplier,
                                                   String message) throws T {
        if (checkFunction.get()) {
            throw exceptionSupplier.apply(message);
        }
    }

}
