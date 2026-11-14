package vn.rms.identity;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.rms.audit.AuditService;

@Service
@Transactional
public class MemberService {
    private final UserAccountRepository users;
    private final OrganizationRepository organizations;
    private final OrganizationMembershipRepository memberships;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public MemberService(UserAccountRepository users, OrganizationRepository organizations,
                         OrganizationMembershipRepository memberships, PasswordEncoder encoder,
                         AuditService audit) {
        this.users = users;
        this.organizations = organizations;
        this.memberships = memberships;
        this.encoder = encoder;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> list(UUID organizationId) {
        return memberships.findAllByOrganization_IdOrderByRoleAsc(organizationId)
                .stream().map(MemberResponse::from).toList();
    }

    public MemberResponse invite(RmsUserPrincipal actor, String email, String password, String fullName, MembershipRole role) {
        if (role == MembershipRole.OWNER || role == MembershipRole.TENANT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use tenant portal provisioning for TENANT accounts");
        }
        if (role == MembershipRole.MANAGER && actor.role() != MembershipRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only organization owner can invite MANAGER");
        }
        MemberResponse created = createMemberAccount(actor.organizationId(), email, password, fullName, role);
        audit.log(actor.organizationId(), actor.userId(), actor.email(), "MEMBER_INVITED",
                "USER", created.userId().toString(),
                "Thêm thành viên " + created.email() + " (" + role.name() + ")");
        return created;
    }

    public MemberResponse createMemberAccount(UUID organizationId, String email, String password,
                                              String fullName, MembershipRole role) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(normalized)) {
            throw new EmailAlreadyRegisteredException();
        }
        UserAccount user = users.save(new UserAccount(normalized, encoder.encode(password), fullName.trim()));
        Organization org = organizations.findById(organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
        OrganizationMembership membership = memberships.save(new OrganizationMembership(org, user, role));
        return MemberResponse.from(membership);
    }

    public MemberResponse setEnabled(RmsUserPrincipal actor, UUID targetUserId, boolean enabled) {
        if (actor.userId().equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot disable your own account");
        }
        OrganizationMembership membership = memberships.findByOrganization_IdAndUser_Id(actor.organizationId(), targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        if (membership.getRole() == MembershipRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot modify organization owner status");
        }
        if (membership.getRole() == MembershipRole.MANAGER && actor.role() != MembershipRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only owner can modify MANAGER account");
        }
        membership.getUser().setEnabled(enabled);
        audit.log(actor.organizationId(), actor.userId(), actor.email(), "MEMBER_STATUS_CHANGED",
                "USER", targetUserId.toString(), "Trạng thái kích hoạt = " + enabled);
        return MemberResponse.from(membership);
    }

    public void resetPassword(RmsUserPrincipal actor, UUID targetUserId, String newPassword) {
        OrganizationMembership membership = memberships.findByOrganization_IdAndUser_Id(actor.organizationId(), targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        membership.getUser().setPasswordHash(encoder.encode(newPassword));
        audit.log(actor.organizationId(), actor.userId(), actor.email(), "MEMBER_PASSWORD_RESET",
                "USER", targetUserId.toString(), "Chủ sở hữu đặt lại mật khẩu cho " + membership.getUser().getEmail());
    }

    public record MemberResponse(UUID userId, String email, String fullName, MembershipRole role, boolean enabled) {
        public static MemberResponse from(OrganizationMembership m) {
            return new MemberResponse(m.getUser().getId(), m.getUser().getEmail(),
                    m.getUser().getFullName(), m.getRole(), m.getUser().isEnabled());
        }
    }
}
