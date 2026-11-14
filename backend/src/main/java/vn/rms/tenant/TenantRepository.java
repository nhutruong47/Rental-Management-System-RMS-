package vn.rms.tenant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    List<Tenant> findAllByOrganizationIdOrderByFullName(UUID organizationId);
    Optional<Tenant> findByOrganizationIdAndId(UUID organizationId, UUID id);
    Optional<Tenant> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);
}
