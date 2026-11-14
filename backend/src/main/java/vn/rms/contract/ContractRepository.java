package vn.rms.contract;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRepository extends JpaRepository<RentalContract, UUID> {
    Optional<RentalContract> findByOrganization_IdAndId(UUID organizationId, UUID contractId);
    List<RentalContract> findAllByOrganization_IdOrderByStartDateDesc(UUID organizationId);
    List<RentalContract> findAllByOrganization_IdAndTenant_IdOrderByStartDateDesc(UUID organizationId, UUID tenantId);
    List<RentalContract> findByOrganization_IdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            UUID organizationId, ContractStatus status, LocalDate endDate, LocalDate startDate);
    boolean existsByRoom_IdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            UUID roomId, ContractStatus status, LocalDate endDate, LocalDate startDate);
}
