package vn.rms.identity;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserAccountRepository users;
    private final OrganizationRepository organizations;
    private final OrganizationMembershipRepository memberships;
    private final PasswordEncoder encoder;

    public AuthService(UserAccountRepository users, OrganizationRepository organizations,
                       OrganizationMembershipRepository memberships, PasswordEncoder encoder) {
        this.users = users;
        this.organizations = organizations;
        this.memberships = memberships;
        this.encoder = encoder;
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
    }
}
