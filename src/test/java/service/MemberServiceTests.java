package service;

import config.Container;
import config.PackageName;
import app.domain.Member;
import app.dto.MemberRegisterDTO;
import app.dto.MemberUpdateDTO;
import exception.MemberEmailAlreadyExistsException;
import exception.MemberIdAlreadyExistsException;
import message.ExceptionMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import app.service.interfaces.MemberServiceInterface;
import util.BCryptPasswordUtil;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class MemberServiceTests {
    private MemberServiceInterface memberService;

    @BeforeEach
    public void setUp() throws Exception {

        Container.getInstance().scan(PackageName.repository.getPackageName());
        Container.getInstance().scan(PackageName.util.getPackageName());
        Container.getInstance().scan(PackageName.service.getPackageName());

        memberService = Container.getInstance().get(MemberServiceInterface.class);
    }

    @Test
    public void testRegisterMember() {
        MemberRegisterDTO registerDTO = new MemberRegisterDTO("yubin11", "yubin", "yubin@gmail.com", "qwer");
        memberService.registerMember(registerDTO);

        Optional<Member> retrievedMember = memberService.getMemberById(registerDTO.getId());
        assertTrue(retrievedMember.isPresent());
        assertEquals("yubin11", retrievedMember.get().getId());
        assertEquals("yubin@gmail.com", retrievedMember.get().getEmail());
    }

    @Test
    public void testRegisterDuplicateId() {
        MemberRegisterDTO registerDTO1 = new MemberRegisterDTO("yubin11", "yubin", "yubin1@gmail.com", "qwer");
        MemberRegisterDTO registerDTO2 = new MemberRegisterDTO("yubin11", "yubin", "yubin2@gmail.com", "qwer");
        memberService.registerMember(registerDTO1);

        Exception exception = assertThrows(MemberIdAlreadyExistsException.class, () -> memberService.registerMember(registerDTO2));
        assertEquals(ExceptionMessage.MEMBER_ID_ALREADY_EXISTS, exception.getMessage());
    }

    @Test
    public void testRegisterDuplicateEmail() {
        MemberRegisterDTO registerDTO1 = new MemberRegisterDTO("yubin111", "yubin", "yubin@gmail.com", "qwer");
        MemberRegisterDTO registerDTO2 = new MemberRegisterDTO("yubin112", "yubin", "yubin@gmail.com", "qwer");
        memberService.registerMember(registerDTO1);

        Exception exception = assertThrows(MemberEmailAlreadyExistsException.class, () -> memberService.registerMember(registerDTO2));
        assertEquals(ExceptionMessage.MEMBER_EMAIL_ALREADY_EXISTS, exception.getMessage());
    }

    @Test
    public void testGetAllMembers() {
        MemberRegisterDTO registerDTO1 = new MemberRegisterDTO("yubin111", "yubin", "yubin1@gmail.com", "qwer");
        MemberRegisterDTO registerDTO2 = new MemberRegisterDTO("yubin112", "yubin", "yubin2@gmail.com", "qwer");
        memberService.registerMember(registerDTO1);
        memberService.registerMember(registerDTO2);

        List<Member> allMembers = memberService.getAllMembers();
        assertEquals(2, allMembers.size());
    }

    @Test
    public void testGetMemberById() {
        MemberRegisterDTO registerDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin1@gmail.com", "qwer");
        memberService.registerMember(registerDTO);
        Optional<Member> retrievedMember = memberService.getMemberById(registerDTO.getId());
        assertTrue(retrievedMember.isPresent());
    }

    @Test
    public void testGetMemberByEmail() {
        MemberRegisterDTO registerDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin1@gmail.com", "qwer");
        memberService.registerMember(registerDTO);
        Optional<Member> retrievedMember = memberService.getMemberByEmail(registerDTO.getEmail());
        assertTrue(retrievedMember.isPresent());
    }

    @Test
    public void testUpdateMember() {
        MemberRegisterDTO registerDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin1@gmail.com", "qwer");
        memberService.registerMember(registerDTO);

        MemberUpdateDTO memberUpdateDTO = new MemberUpdateDTO("yubin2@gmail.com", null);
        memberService.updateMember(registerDTO.getId(), memberUpdateDTO);

        Optional<Member> retrievedMember = memberService.getMemberById(registerDTO.getId());
        assertEquals("yubin2@gmail.com", retrievedMember.orElseThrow().getEmail());

        MemberUpdateDTO memberUpdateDTO2 = new MemberUpdateDTO(null, "tyui");
        memberService.updateMember(registerDTO.getId(), memberUpdateDTO2);

        retrievedMember = memberService.getMemberById(registerDTO.getId());
        assertEquals("yubin2@gmail.com", retrievedMember.orElseThrow().getEmail());
        assertTrue(BCryptPasswordUtil.matchPassword("tyui", retrievedMember.get().getEncryptedPassword()));
    }

    @Test
    public void testDeleteMember() {
        MemberRegisterDTO registerDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin1@gmail.com", "qwer");
        memberService.registerMember(registerDTO);
        memberService.deleteMember(registerDTO.getId());
        assertEquals(0, memberService.getAllMembers().size());
        assertFalse(memberService.getMemberById(registerDTO.getId()).isPresent());
    }

}
