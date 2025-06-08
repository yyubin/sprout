package app.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class MemberRegisterDTO {
    private String id;
    private String name;
    private String email;
    private String password;
    private LocalDate joinDate;

    public MemberRegisterDTO() {
    }

    public MemberRegisterDTO(String id, String name, String email, String password) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
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

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
