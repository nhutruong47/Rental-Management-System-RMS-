package vn.rms.utility;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;
import vn.rms.identity.Organization;
import vn.rms.property.Room;

@Entity
@Table(name = "meter_readings")
public class MeterReading {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(name = "utility_type", nullable = false, length = 20)
    private UtilityType utilityType;

    @Column(name = "billing_period", nullable = false, length = 7)
    private String billingPeriod;

    @Column(name = "previous_reading", nullable = false, precision = 19, scale = 3)
    private BigDecimal previousReading;

    @Column(name = "current_reading", nullable = false, precision = 19, scale = 3)
    private BigDecimal currentReading;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal charge;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected MeterReading() { }

    public MeterReading(Organization organization, Room room, UtilityType utilityType, YearMonth period,
                        BigDecimal previousReading, BigDecimal currentReading, BigDecimal unitPrice, BigDecimal charge) {
        this.organization = organization;
        this.room = room;
        this.utilityType = utilityType;
        this.billingPeriod = period.toString();
        this.previousReading = previousReading;
        this.currentReading = currentReading;
        this.unitPrice = unitPrice;
        this.charge = charge;
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organization.getId(); }
    public UUID getRoomId() { return room.getId(); }
    public UtilityType getUtilityType() { return utilityType; }
    public String getBillingPeriod() { return billingPeriod; }
    public BigDecimal getPreviousReading() { return previousReading; }
    public BigDecimal getCurrentReading() { return currentReading; }
    public BigDecimal getUsage() { return currentReading.subtract(previousReading); }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getCharge() { return charge; }
}
