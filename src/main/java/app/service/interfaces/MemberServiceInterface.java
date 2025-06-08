package app.service.interfaces;

import app.domain.Member;
import app.dto.MemberRegisterDTO;
import app.dto.MemberUpdateDTO;
import app.exception.MemberEmailAlreadyExistsException;
import app.exception.MemberIdAlreadyExistsException;
import app.exception.MemberNotFoundException;

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
