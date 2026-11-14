package vn.rms.tenant;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.rms.audit.AuditService;
import vn.rms.identity.MemberService;
import vn.rms.identity.MembershipRole;
import vn.rms.identity.RmsUserPrincipal;

@Service
@Transactional
public class TenantService {
    private final TenantRepository tenants;
    private final MemberService members;
    private final AuditService audit;

    public TenantService(TenantRepository tenants, MemberService members, AuditService audit) {
        this.tenants = tenants;
        this.members = members;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<Tenant> list(UUID organizationId) {
        return tenants.findAllByOrganizationIdOrderByFullName(organizationId);
    }

    public Tenant create(UUID organizationId, String fullName, String phone, String email) {
        return tenants.save(new Tenant(organizationId, fullName.trim(), clean(phone), clean(email)));
    }

    public MemberService.MemberResponse provisionPortalAccount(RmsUserPrincipal actor, UUID tenantId,
                                                               String email, String password) {
        Tenant tenant = tenants.findByOrganizationIdAndId(actor.organizationId(), tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
        if (tenant.getUserId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant already has a portal account");
        }
        MemberService.MemberResponse created = members.createMemberAccount(
                actor.organizationId(), email, password, tenant.getFullName(), MembershipRole.TENANT);
        tenant.linkUser(created.userId());
        audit.log(actor.organizationId(), actor.userId(), actor.email(), "TENANT_PORTAL_PROVISIONED",
                "TENANT", tenant.getId().toString(),
                "Cấp tài khoản cổng người thuê: " + created.email());
        return created;
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
