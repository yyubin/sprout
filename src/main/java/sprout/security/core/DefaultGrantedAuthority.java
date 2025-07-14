package sprout.security.core;

public class DefaultGrantedAuthority implements GrantedAuthority{
    private final String authority;

    public DefaultGrantedAuthority(String authority) {
        this.authority = authority;
    }

    @Override
    public String getAuthority() {
        return this.authority;
    }
}
