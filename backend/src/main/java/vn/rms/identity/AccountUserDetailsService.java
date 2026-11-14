package vn.rms.identity;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountUserDetailsService implements UserDetailsService {
    private final UserAccountRepository users;
    private final OrganizationMembershipRepository memberships;

    public AccountUserDetailsService(UserAccountRepository users, OrganizationMembershipRepository memberships) {
        this.users = users;
        this.memberships = memberships;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserAccount user = users.findByEmailIgnoreCase(email.trim()).orElseThrow(() ->
                new UsernameNotFoundException("Invalid email or password"));
        OrganizationMembership membership = memberships.findFirstByUserId(user.getId()).orElseThrow(() ->
                new UsernameNotFoundException("Invalid email or password"));
        return new RmsUserPrincipal(user.getId(), membership.getOrganization().getId(),
                membership.getOrganization().getName(), user.getEmail(),
                user.getPasswordHash(), user.isEnabled(), membership.getRole());
    }
}
