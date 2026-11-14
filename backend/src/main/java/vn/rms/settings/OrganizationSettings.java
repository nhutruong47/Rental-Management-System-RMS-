package vn.rms.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organization_settings")
public class OrganizationSettings {
    @Id
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "billing_due_day", nullable = false)
    private short billingDueDay = 5;

    @Column(name = "internet_fee", nullable = false, precision = 19, scale = 0)
    private BigDecimal internetFee = BigDecimal.ZERO;

    @Column(name = "garbage_fee", nullable = false, precision = 19, scale = 0)
    private BigDecimal garbageFee = BigDecimal.ZERO;

    @Column(name = "parking_fee", nullable = false, precision = 19, scale = 0)
    private BigDecimal parkingFee = BigDecimal.ZERO;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected OrganizationSettings() { }

    public OrganizationSettings(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public void update(short billingDueDay, BigDecimal internetFee, BigDecimal garbageFee, BigDecimal parkingFee) {
        this.billingDueDay = billingDueDay;
        this.internetFee = internetFee;
        this.garbageFee = garbageFee;
        this.parkingFee = parkingFee;
        this.updatedAt = Instant.now();
    }

    public UUID getOrganizationId() { return organizationId; }
    public short getBillingDueDay() { return billingDueDay; }
    public BigDecimal getInternetFee() { return internetFee; }
    public BigDecimal getGarbageFee() { return garbageFee; }
    public BigDecimal getParkingFee() { return parkingFee; }
    public BigDecimal totalRecurringFees() { return internetFee.add(garbageFee).add(parkingFee); }
}
