package vn.rms.billing;

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
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;
import vn.rms.contract.RentalContract;
import vn.rms.identity.Organization;

@Entity
@Table(name = "invoices", uniqueConstraints = @jakarta.persistence.UniqueConstraint(
        name = "uk_invoice_org_contract_period", columnNames = {"organization_id", "contract_id", "billing_period"}))
public class RentalInvoice {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private RentalContract contract;

    @Column(name = "billing_period", nullable = false, length = 7)
    private String billingPeriod;

    @Column(name = "rent_amount", nullable = false, precision = 19, scale = 0)
    private BigDecimal rentAmount;

    @Column(name = "electricity_amount", nullable = false, precision = 19, scale = 0)
    private BigDecimal electricityAmount;

    @Column(name = "water_amount", nullable = false, precision = 19, scale = 0)
    private BigDecimal waterAmount;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 0)
    private BigDecimal totalAmount;

    @Column(name = "amount_paid", nullable = false, precision = 19, scale = 0)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected RentalInvoice() { }

    public RentalInvoice(Organization organization, RentalContract contract, YearMonth period,
                         BigDecimal rent, BigDecimal electricity, BigDecimal water) {
        this.organization = organization;
        this.contract = contract;
        this.billingPeriod = period.toString();
        this.rentAmount = rent;
        this.electricityAmount = electricity;
        this.waterAmount = water;
        this.totalAmount = rent.add(electricity).add(water);
    }

    public void issue() {
        if (status != InvoiceStatus.DRAFT) throw new IllegalStateException("Only draft invoices can be issued");
        status = InvoiceStatus.ISSUED;
        issuedAt = Instant.now();
    }

    public void applyPayment(BigDecimal amount) {
        if (status != InvoiceStatus.ISSUED && status != InvoiceStatus.PARTIALLY_PAID) {
            throw new IllegalStateException("Invoice is not payable");
        }
        if (amount.signum() <= 0 || amount.compareTo(getBalanceDue()) > 0) {
            throw new IllegalArgumentException("Payment must be positive and not exceed the balance due");
        }
        amountPaid = amountPaid.add(amount);
        status = amountPaid.compareTo(totalAmount) == 0 ? InvoiceStatus.PAID : InvoiceStatus.PARTIALLY_PAID;
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organization.getId(); }
    public UUID getContractId() { return contract.getId(); }
    public UUID getRoomId() { return contract.getRoomId(); }
    public UUID getTenantId() { return contract.getTenantId(); }
    public String getBillingPeriod() { return billingPeriod; }
    public BigDecimal getRentAmount() { return rentAmount; }
    public BigDecimal getElectricityAmount() { return electricityAmount; }
    public BigDecimal getWaterAmount() { return waterAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getAmountPaid() { return amountPaid; }
    public BigDecimal getBalanceDue() { return totalAmount.subtract(amountPaid); }
    public InvoiceStatus getStatus() { return status; }
}
