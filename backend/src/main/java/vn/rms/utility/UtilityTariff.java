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
import java.time.LocalDate;
import java.util.UUID;
import vn.rms.identity.Organization;

@Entity
@Table(name = "utility_tariffs")
public class UtilityTariff {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "utility_type", nullable = false, length = 20)
    private UtilityType utilityType;

    @Column(name = "price_per_unit", nullable = false, precision = 19, scale = 4)
    private BigDecimal pricePerUnit;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    protected UtilityTariff() { }

    public UtilityTariff(Organization organization, UtilityType utilityType, BigDecimal pricePerUnit, LocalDate effectiveFrom) {
        this.organization = organization;
        this.utilityType = utilityType;
        this.pricePerUnit = pricePerUnit;
        this.effectiveFrom = effectiveFrom;
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organization.getId(); }
    public UtilityType getUtilityType() { return utilityType; }
    public BigDecimal getPricePerUnit() { return pricePerUnit; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
}
