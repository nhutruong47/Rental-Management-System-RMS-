package vn.rms.utility;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeterReadingRepository extends JpaRepository<MeterReading, UUID> {
    Optional<MeterReading> findByOrganization_IdAndRoom_IdAndUtilityTypeAndBillingPeriod(
            UUID organizationId, UUID roomId, UtilityType utilityType, String billingPeriod);
    Optional<MeterReading> findFirstByOrganization_IdAndRoom_IdAndUtilityTypeAndBillingPeriodLessThanOrderByBillingPeriodDesc(
            UUID organizationId, UUID roomId, UtilityType utilityType, String billingPeriod);
    Optional<MeterReading> findFirstByOrganization_IdAndRoom_IdAndUtilityTypeAndBillingPeriodGreaterThanOrderByBillingPeriodAsc(
            UUID organizationId, UUID roomId, UtilityType utilityType, String billingPeriod);
    List<MeterReading> findByOrganization_IdAndBillingPeriodOrderByRoom_IdAsc(UUID organizationId, String billingPeriod);
}
