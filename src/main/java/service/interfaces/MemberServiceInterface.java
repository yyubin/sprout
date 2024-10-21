package service.interfaces;

import domain.Member;
import dto.MemberRegisterDTO;
import dto.MemberUpdateDTO;
import exception.MemberEmailAlreadyExistsException;
import exception.MemberIdAlreadyExistsException;
import exception.MemberNotFoundException;

import java.util.List;
import java.util.Optional;

public interface MemberServiceInterface {

    void registerMember(MemberRegisterDTO memberRegisterDTO)
            throws MemberIdAlreadyExistsException, MemberEmailAlreadyExistsException;

    void registerAdminMember();

    Optional<Member> getMemberById(String memberId);

    Optional<Member> getMemberByEmail(String email);

    List<Member> getAllMembers();

    void updateMember(String memberId, MemberUpdateDTO memberUpdateDTO)
            throws MemberNotFoundException;

    void deleteMember(String memberId) throws MemberNotFoundException;

}
