package vn.rms.identity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService members;

    public MemberController(MemberService members) {
        this.members = members;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public List<MemberService.MemberResponse> list(@AuthenticationPrincipal RmsUserPrincipal principal) {
        return members.list(principal.organizationId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public MemberService.MemberResponse invite(@AuthenticationPrincipal RmsUserPrincipal principal,
                                               @Valid @RequestBody InviteMemberRequest request) {
        return members.invite(principal, request.email(), request.password(), request.fullName(), request.role());
    }

    @PostMapping("/{userId}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public MemberService.MemberResponse updateStatus(@AuthenticationPrincipal RmsUserPrincipal principal,
                                                     @PathVariable UUID userId,
                                                     @Valid @RequestBody UpdateMemberStatusRequest request) {
        return members.setEnabled(principal, userId, request.enabled());
    }

    @PostMapping("/{userId}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('OWNER')")
    public void resetPassword(@AuthenticationPrincipal RmsUserPrincipal principal,
                              @PathVariable UUID userId,
                              @Valid @RequestBody ResetPasswordRequest request) {
        members.resetPassword(principal, userId, request.newPassword());
    }

    public record InviteMemberRequest(@Email @NotBlank @Size(max = 254) String email,
                                      @NotBlank @Size(min = 12, max = 72) String password,
                                      @NotBlank @Size(max = 120) String fullName,
                                      @NotNull MembershipRole role) { }

    public record UpdateMemberStatusRequest(boolean enabled) { }

    public record ResetPasswordRequest(@NotBlank @Size(min = 12, max = 72) String newPassword) { }
}
