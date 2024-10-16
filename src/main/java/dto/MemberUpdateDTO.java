package dto;

public class MemberUpdateDTO {

    private String email;
    private String password;

    public MemberUpdateDTO(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
