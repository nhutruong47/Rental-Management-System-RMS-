package vn.rms.property;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyRepository extends JpaRepository<Property, UUID> {
    List<Property> findAllByOrganizationIdOrderByName(UUID organizationId);
    Optional<Property> findByIdAndOrganizationId(UUID id, UUID organizationId);
    boolean existsByOrganizationIdAndNameIgnoreCaseAndAddressIgnoreCase(UUID organizationId, String name, String address);
}
