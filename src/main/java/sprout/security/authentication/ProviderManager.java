package sprout.security.authentication;

import sprout.beans.annotation.Component;
import sprout.security.authentication.exception.*;
import sprout.security.core.Authentication;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ProviderManager implements AuthenticationManager{
    private final AuthenticationEventPublisher eventPublisher;
    private final List<AuthenticationProvider> providers;
    private final AuthenticationManager parent;

    public ProviderManager(AuthenticationEventPublisher eventPublisher, List<AuthenticationProvider> providers, AuthenticationManager parent) {
        // null 방지 및 불변 리스트로 설정
        this.eventPublisher = Objects.requireNonNullElseGet(eventPublisher, NullEventPublisher::new);
        this.providers = Optional.ofNullable(providers).orElse(Collections.emptyList());
        this.parent = parent;
    }

    public ProviderManager(List<AuthenticationProvider> providers) {
        this(new NullEventPublisher(), providers, null);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws LoginException {
        Class<? extends Authentication> toTest = authentication.getClass();
        LoginException lastException = null;

        for (AuthenticationProvider provider : this.providers) {
            if (!provider.supports(toTest)) {
                continue;
            }

            try {
                Authentication result = provider.authenticate(authentication);
                if (result != null) {
                    eventPublisher.publishAuthenticationSuccess(result);
                    return result; // 성공적으로 인증되었으므로 결과 반환
                }
            } catch (LoginException ex) {
                if (ex instanceof AccountExpiredException || ex instanceof CredentialExpiredException) {
                    eventPublisher.publishAuthenticationFailure(ex, authentication);
                    throw ex; // 계정 상태 관련 예외는 즉시 던짐 (스프링과 유사)
                }

                // 그 외 인증 실패 예외는 저장해두고 다음 Provider 시도
                lastException = ex;
                System.out.println("Authentication failed with provider " + provider.getClass().getSimpleName() + ": " + ex.getMessage());
            } catch (Exception ex) {
                lastException = new AuthenticationException("Internal authentication service error: " + ex.getMessage());
                eventPublisher.publishAuthenticationFailure(lastException, authentication);
                throw lastException;
            }
        }

        // 현재 Provider들로 인증에 실패했고, 부모 AuthenticationManager가 있다면 위임
        if (lastException != null && this.parent != null) {
            try {
                Authentication parentResult = this.parent.authenticate(authentication);
                if (parentResult != null) {
                    eventPublisher.publishAuthenticationSuccess(parentResult);
                    return parentResult; // 부모가 성공하면 결과 반환
                }
            } catch (LoginException ex) {
                lastException = ex;
            }
        } else if (this.parent != null) { // 현재 Provider가 없거나 지원하는 Provider가 없는 경우
            // 모든 Provider가 지원하지 않았고, 부모가 있다면 부모에게 위임
            try {
                Authentication parentResult = this.parent.authenticate(authentication);
                if (parentResult != null) {
                    eventPublisher.publishAuthenticationSuccess(parentResult);
                    return parentResult; // 부모가 성공하면 결과 반환
                }
            } catch (LoginException ex) {
                lastException = ex;
            }
        }

        // 모든 시도가 실패했을 때 최종 예외 던지기
        if (lastException == null) {
            throw new ProviderNotFoundException("No AuthenticationProvider found for " + toTest.getName());
        } else {
            eventPublisher.publishAuthenticationFailure(lastException, authentication);
            throw lastException;
        }
    }

    // 테스트나 디버깅을 위한 getter
    public List<AuthenticationProvider> getProviders() {
        return providers;
    }

    private static final class NullEventPublisher implements AuthenticationEventPublisher {

        @Override
        public void publishAuthenticationFailure(LoginException exception, Authentication authentication) {
        }

        @Override
        public void publishAuthenticationSuccess(Authentication authentication) {
        }

    }
}
