package dto;

public class MemberLoginDTO {

    private String id;
    private String password;

    public MemberLoginDTO(String id, String password) {
        this.id = id;
        this.password = password;
    }

    public String getId() {
        return id;
    }

    public String getPassword() {
        return password;
    }
}
