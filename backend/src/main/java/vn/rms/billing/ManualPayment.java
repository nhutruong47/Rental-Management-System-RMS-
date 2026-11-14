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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import vn.rms.identity.Organization;

@Entity
@Table(name = "payments")
public class ManualPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private RentalInvoice invoice;

    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PaymentMethod method;

    @Column(nullable = false, length = 120)
    private String reference;

    @Column(name = "idempotency_key", nullable = false, length = 80)
    private String idempotencyKey;

    @Column(name = "recorded_by_user_id", nullable = false)
    private UUID recordedByUserId;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt = Instant.now();

    protected ManualPayment() { }

    public ManualPayment(Organization organization, RentalInvoice invoice, BigDecimal amount,
                         PaymentMethod method, String reference, String idempotencyKey, UUID recordedByUserId) {
        this.organization = organization;
        this.invoice = invoice;
        this.amount = amount;
        this.method = method;
        this.reference = reference;
        this.idempotencyKey = idempotencyKey;
        this.recordedByUserId = recordedByUserId;
    }

    public UUID getId() { return id; }
    public UUID getInvoiceId() { return invoice.getId(); }
    public BigDecimal getAmount() { return amount; }
    public PaymentMethod getMethod() { return method; }
    public String getReference() { return reference; }
    public Instant getRecordedAt() { return recordedAt; }
}
