package vn.rms.billing;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<ManualPayment, UUID> {
    Optional<ManualPayment> findByOrganization_IdAndIdempotencyKey(UUID organizationId, String idempotencyKey);
}
