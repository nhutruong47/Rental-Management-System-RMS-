package vn.rms.billing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<RentalInvoice, UUID> {
    List<RentalInvoice> findByOrganization_IdAndBillingPeriodOrderByIdAsc(UUID organizationId, String billingPeriod);
    List<RentalInvoice> findByOrganization_IdAndContract_Id(UUID organizationId, UUID contractId);
    List<RentalInvoice> findAllByOrganization_IdAndContract_Tenant_IdOrderByBillingPeriodDesc(UUID organizationId, UUID tenantId);
    Optional<RentalInvoice> findByOrganization_IdAndId(UUID organizationId, UUID invoiceId);
    boolean existsByOrganization_IdAndContract_IdAndBillingPeriod(UUID organizationId, UUID contractId, String billingPeriod);
}
