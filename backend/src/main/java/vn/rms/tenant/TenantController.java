package vn.rms.tenant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
import vn.rms.identity.MemberService;
import vn.rms.identity.RmsUserPrincipal;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {
    private final TenantService tenants;

    public TenantController(TenantService tenants) {
        this.tenants = tenants;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
    public List<TenantResponse> list(@AuthenticationPrincipal RmsUserPrincipal principal) {
        return tenants.list(principal.organizationId()).stream().map(TenantResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public TenantResponse create(@AuthenticationPrincipal RmsUserPrincipal principal,
                                 @Valid @RequestBody CreateTenantRequest request) {
        return TenantResponse.from(tenants.create(principal.organizationId(), request.fullName(), request.phone(), request.email()));
    }

    @PostMapping("/{tenantId}/portal-account")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public MemberService.MemberResponse provisionPortal(@AuthenticationPrincipal RmsUserPrincipal principal,
                                                        @PathVariable UUID tenantId,
                                                        @Valid @RequestBody ProvisionPortalRequest request) {
        return tenants.provisionPortalAccount(principal, tenantId, request.email(), request.password());
    }

    public record CreateTenantRequest(@NotBlank @Size(max = 120) String fullName,
                                      @Size(max = 40) String phone,
                                      @Email @Size(max = 254) String email) { }

    public record ProvisionPortalRequest(@Email @NotBlank @Size(max = 254) String email,
                                         @NotBlank @Size(min = 12, max = 72) String password) { }

    public record TenantResponse(UUID id, String fullName, String phone, String email, boolean hasPortalAccount) {
        static TenantResponse from(Tenant tenant) {
            return new TenantResponse(tenant.getId(), tenant.getFullName(), tenant.getPhone(),
                    tenant.getEmail(), tenant.getUserId() != null);
        }
    }
}
