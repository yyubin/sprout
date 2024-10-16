package dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class MemberRegisterDTO {
    private String id;
    private String name;
    private String email;
    private String password;
    private LocalDate joinDate;

    public MemberRegisterDTO(String id, String name, String email, String password, LocalDate joinDate) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.joinDate = joinDate;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public LocalDate getJoinDate() {
        return joinDate;
    }
}
