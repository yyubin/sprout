package sprout.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import sprout.beans.annotation.Component;
import sprout.core.filter.Filter;
import sprout.core.filter.FilterChain;
import sprout.mvc.http.*;
import sprout.security.authentication.AuthenticationManager;
import sprout.security.authentication.UsernamePasswordAuthenticationToken;
import sprout.security.authentication.exception.LoginException;
import sprout.security.web.util.matcher.RequestMatcher;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public class AuthenticationFilter implements Filter {

    private final RequestMatcher requestMatcher;
    private final AuthenticationManager authenticationManager;
    private final ObjectMapper objectMapper;

    public AuthenticationFilter(RequestMatcher requestMatcher, AuthenticationManager authenticationManager) {
        this.requestMatcher = requestMatcher;
        this.authenticationManager = authenticationManager;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) throws IOException {
        if (requestMatcher.matches(request)) {
            try {
                if (request.getMethod() != HttpMethod.POST) {
                    ResponseEntity<?> responseEntity = new ResponseEntity<>("Method Not Allowed. Only POST is supported for login.", null, ResponseCode.METHOD_NOT_ALLOWED);
                    response.setResponseEntity(responseEntity);
                    return;
                }

                Map<String, Object> requestBody = objectMapper.readValue((String) request.getBody(), Map.class);

                String username = Optional.ofNullable((String) requestBody.get("username"))
                        .orElseThrow(() -> new LoginException("Username not provided."));
                String password = Optional.ofNullable((String) requestBody.get("password"))
                        .orElseThrow(() -> new LoginException("Password not provided."));

                UsernamePasswordAuthenticationToken unauthenticatedToken = new UsernamePasswordAuthenticationToken(
                        username,
                        password
                );
                authenticationManager.authenticate(unauthenticatedToken);

                ResponseEntity<?> responseEntity = new ResponseEntity<>("Authentication successful!", null, ResponseCode.SUCCESS);
                response.setResponseEntity(responseEntity);

            } catch (LoginException e) {
                // TODO: 인증 실패 시 처리 (예: 401 Unauthorized 응답, 에러 메시지 반환)
                ResponseEntity<?> responseEntity = new ResponseEntity<>("Authentication failed: " + e.getMessage(), null, ResponseCode.UNAUTHORIZED);
                response.setResponseEntity(responseEntity);
            } catch (IOException | ClassCastException e) { // JSON 파싱 에러 또는 Body 타입 불일치
                ResponseEntity<?> responseEntity = new ResponseEntity<>("Invalid request body format.", null, ResponseCode.BAD_REQUEST);
                response.setResponseEntity(responseEntity);
            } catch (Exception e) {
                ResponseEntity<?> responseEntity = new ResponseEntity<>("An internal server error occurred during authentication.", null, ResponseCode.BAD_REQUEST);
                response.setResponseEntity(responseEntity);
            }
            return;
        }

        chain.doFilter(request, response);
    }
}
