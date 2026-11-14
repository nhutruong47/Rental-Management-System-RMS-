package vn.rms.contract;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HandoverRecordRepository extends JpaRepository<HandoverRecord, UUID> {
    Optional<HandoverRecord> findByOrganization_IdAndContract_Id(UUID organizationId, UUID contractId);
}
