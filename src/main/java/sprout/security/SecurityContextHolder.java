package sprout.security;

public final class SecurityContextHolder {
    private static final ThreadLocal<UserPrincipal> ctx = new ThreadLocal<>();
    public static void set(UserPrincipal p)  { ctx.set(p); }
    public static UserPrincipal get()        { return ctx.get(); }
    public static void clear()               { ctx.remove(); }
}
