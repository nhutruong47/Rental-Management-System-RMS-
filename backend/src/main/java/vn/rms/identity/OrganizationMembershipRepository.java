package vn.rms.identity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, UUID> {
    @EntityGraph(attributePaths = "organization")
    Optional<OrganizationMembership> findFirstByUserId(UUID userId);

    @EntityGraph(attributePaths = "user")
    List<OrganizationMembership> findAllByOrganization_IdOrderByRoleAsc(UUID organizationId);

    @EntityGraph(attributePaths = {"user", "organization"})
    Optional<OrganizationMembership> findByOrganization_IdAndUser_Id(UUID organizationId, UUID userId);
}
