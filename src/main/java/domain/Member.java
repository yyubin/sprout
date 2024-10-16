package domain;

import java.time.LocalDate;

public class Member {

    private String id;
    private String name;
    private String email;
    private LocalDate joinDate;
    private boolean isWithdrawn;
    private String encryptedPassword;
    private MemberGrade grade;

    public Member(String id, String name, String email, LocalDate joinDate, boolean isWithdrawn, String encryptedPassword, MemberGrade grade) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.joinDate = joinDate;
        this.isWithdrawn = isWithdrawn;
        this.encryptedPassword = encryptedPassword;
        this.grade = grade;
    }

    public String getName() {
        return name;
    }

}
