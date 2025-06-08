package app.domain;

import app.domain.grade.MemberGrade;
import util.BCryptPasswordUtil;

import java.time.LocalDate;

public class Member {

    private String id;
    private String name;
    private String email;
    private LocalDate joinDate;
    private boolean deleted;
    private String encryptedPassword;
    private MemberGrade grade;

    public Member(String id, String name, String email, LocalDate joinDate, String encryptedPassword) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.joinDate = joinDate;
        this.deleted = false;
        this.encryptedPassword = encryptedPassword;
        this.grade = MemberGrade.USER;
    }

    private Member() {
        this.id = "admin";
        this.name = "admin";
        this.email = "admin@gmail.com";
        this.joinDate = LocalDate.now();
        this.deleted = false;
        this.encryptedPassword = BCryptPasswordUtil.encryptPassword("admin");
        this.grade = MemberGrade.ADMIN;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public String getEmail() {
        return email;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public LocalDate getJoinDate() {
        return joinDate;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public MemberGrade getGrade() {
        return grade;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public static Member makeAdminForTest() {
        return new Member();
    }
}
