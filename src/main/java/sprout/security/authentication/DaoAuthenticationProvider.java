package sprout.security.authentication;

import sprout.security.authentication.exception.AccountExpiredException;
import sprout.security.authentication.exception.BadCredentialsException;
import sprout.security.authentication.exception.CredentialExpiredException;
import sprout.security.authentication.exception.UsernameNotFoundException;
import sprout.security.authentication.password.PasswordEncoder;
import sprout.security.core.Authentication;
import sprout.security.core.UserDetails;
import sprout.security.core.UserDetailsService;

import javax.naming.AuthenticationException;

public class DaoAuthenticationProvider implements AuthenticationProvider{

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public DaoAuthenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        UsernamePasswordAuthenticationToken unauthenticatedToken = (UsernamePasswordAuthenticationToken) authentication;
        String username = unauthenticatedToken.getPrincipal().toString();
        String rawPassword = unauthenticatedToken.getCredentials().toString();

        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException notFound) {
            System.out.println("User '" + username + "' not found.");
            throw new BadCredentialsException("Bad credentials", notFound);
        }

        if (!passwordEncoder.matches(rawPassword, userDetails.getPassword())) {
            System.out.println("Invalid password for user '" + username + "'");
            throw new BadCredentialsException("Bad credentials");
        }

        if (!userDetails.isAccountNonExpired()) {
            throw new AccountExpiredException("User account has expired");
        }
        if (!userDetails.isAccountNonLocked()) {
            throw new AccountExpiredException("User account is locked");
        }
        if (!userDetails.isCredentialsNonExpired()) {
            throw new CredentialExpiredException("User credentials have expired");
        }
        if (!userDetails.isEnabled()) {
            throw new AccountExpiredException("User account is disabled");
        }

        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
