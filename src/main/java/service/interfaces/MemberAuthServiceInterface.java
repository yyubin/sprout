package service.interfaces;

import domain.grade.MemberGrade;
import dto.MemberLoginDTO;

public interface MemberAuthServiceInterface {
    String login(MemberLoginDTO memberLoginDTO) throws Throwable;
    void logout() throws Throwable;
    MemberGrade checkAuthority(String sessionId) throws Throwable;
}
