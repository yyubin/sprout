package domain;

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

    public void setEmail(String email) {
        this.email = email;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
