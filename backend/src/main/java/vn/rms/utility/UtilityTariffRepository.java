package vn.rms.utility;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UtilityTariffRepository extends JpaRepository<UtilityTariff, UUID> {
    List<UtilityTariff> findAllByOrganization_IdOrderByEffectiveFromDesc(UUID organizationId);
    Optional<UtilityTariff> findFirstByOrganization_IdAndUtilityTypeAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
            UUID organizationId, UtilityType utilityType, LocalDate effectiveOn);
}
