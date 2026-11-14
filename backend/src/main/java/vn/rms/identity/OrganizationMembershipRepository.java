package vn.rms.identity;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, UUID> {
    @EntityGraph(attributePaths = "organization")
    Optional<OrganizationMembership> findFirstByUserId(UUID userId);
}
