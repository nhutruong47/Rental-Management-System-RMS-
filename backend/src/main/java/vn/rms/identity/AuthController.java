package vn.rms.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import vn.rms.audit.AuditService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final AuthRateLimiter rateLimiter;
    private final AuditService audit;

    public AuthController(AuthService authService, AuthenticationManager authenticationManager,
                          AuthRateLimiter rateLimiter, AuditService audit) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.rateLimiter = rateLimiter;
        this.audit = audit;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getToken(), token.getHeaderName());
    }

    @GetMapping("/me")
    public LoginResponse me(@AuthenticationPrincipal RmsUserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return LoginResponse.from(principal);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request.email(), request.password(), request.fullName(), request.organizationName());
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest,
                               HttpServletResponse servletResponse) {
        String key = (servletRequest.getRemoteAddr() == null ? "local" : servletRequest.getRemoteAddr())
                + "|" + request.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (rateLimiter.isBlocked(key)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed login attempts. Please wait a minute and try again.");
        }
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
        } catch (org.springframework.security.core.AuthenticationException ex) {
            rateLimiter.recordFailure(key);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        rateLimiter.clear(key);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        servletRequest.getSession(true);
        new HttpSessionSecurityContextRepository().saveContext(context, servletRequest, servletResponse);
        RmsUserPrincipal principal = (RmsUserPrincipal) authentication.getPrincipal();
        audit.log(principal.organizationId(), principal.userId(), principal.email(), "LOGIN",
                "SESSION", principal.userId().toString(), "Đăng nhập (" + principal.role().name() + ")");
        return LoginResponse.from(principal);
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal RmsUserPrincipal principal,
                               @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal, request.currentPassword(), request.newPassword());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public record RegisterRequest(@Email @NotBlank @Size(max = 254) String email,
                                  @NotBlank @Size(min = 12, max = 72) String password,
                                  @NotBlank @Size(max = 120) String fullName,
                                  @NotBlank @Size(max = 120) String organizationName) { }

    public record LoginRequest(@Email @NotBlank @Size(max = 254) String email,
                               @NotBlank @Size(max = 72) String password) { }

    public record ChangePasswordRequest(@NotBlank @Size(max = 72) String currentPassword,
                                        @NotBlank @Size(min = 12, max = 72) String newPassword) { }

    public record CsrfResponse(String token, String headerName) { }

    public record LoginResponse(java.util.UUID userId, java.util.UUID organizationId, String organizationName,
                                String email, MembershipRole role) {
        static LoginResponse from(RmsUserPrincipal principal) {
            return new LoginResponse(principal.userId(), principal.organizationId(), principal.organizationName(),
                    principal.email(), principal.role());
        }
    }
}
