package vn.rms.identity;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.rms.audit.AuditService;

@Service
public class AuthService {
    private final UserAccountRepository users;
    private final OrganizationRepository organizations;
    private final OrganizationMembershipRepository memberships;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public AuthService(UserAccountRepository users, OrganizationRepository organizations,
                       OrganizationMembershipRepository memberships, PasswordEncoder encoder,
                       AuditService audit) {
        this.users = users;
        this.organizations = organizations;
        this.memberships = memberships;
        this.encoder = encoder;
        this.audit = audit;
    }

    @Transactional
    public void register(String email, String password, String fullName, String organizationName) {
        String normalizedEmail = email.trim().toLowerCase(java.util.Locale.ROOT);
        if (users.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException();
        }
        UserAccount user = users.save(new UserAccount(normalizedEmail, encoder.encode(password), fullName.trim()));
        Organization organization = organizations.save(new Organization(organizationName.trim()));
        memberships.save(new OrganizationMembership(organization, user, MembershipRole.OWNER));
        audit.log(organization.getId(), user.getId(), normalizedEmail, "OWNER_REGISTERED",
                "ORGANIZATION", organization.getId().toString(),
                "Đăng ký tổ chức mới: " + organization.getName());
    }

    @Transactional
    public void changePassword(RmsUserPrincipal principal, String currentPassword, String newPassword) {
        UserAccount user = users.findById(principal.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!encoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }
        user.setPasswordHash(encoder.encode(newPassword));
        audit.log(principal.organizationId(), principal.userId(), principal.email(), "PASSWORD_CHANGED",
                "USER", principal.userId().toString(), "Người dùng tự đổi mật khẩu");
    }
}
