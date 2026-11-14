package vn.rms.contract;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import vn.rms.identity.Organization;

@Entity
@Table(name = "handover_records")
public class HandoverRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private RentalContract contract;

    @Column(name = "terminated_date", nullable = false)
    private LocalDate terminatedDate;

    @Column(name = "final_electricity_reading", precision = 19, scale = 3)
    private BigDecimal finalElectricityReading;

    @Column(name = "final_water_reading", precision = 19, scale = 3)
    private BigDecimal finalWaterReading;

    @Column(name = "outstanding_balance", nullable = false, precision = 19, scale = 0)
    private BigDecimal outstandingBalance;

    @Column(name = "deposit_held", nullable = false, precision = 19, scale = 0)
    private BigDecimal depositHeld;

    @Column(name = "deposit_deducted", nullable = false, precision = 19, scale = 0)
    private BigDecimal depositDeducted;

    @Column(name = "deposit_refunded", nullable = false, precision = 19, scale = 0)
    private BigDecimal depositRefunded;

    @Column(name = "checklist_notes", length = 1000)
    private String checklistNotes;

    @Column(name = "recorded_by_user_id", nullable = false)
    private UUID recordedByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected HandoverRecord() { }

    public HandoverRecord(Organization organization, RentalContract contract, LocalDate terminatedDate,
                          BigDecimal finalElectricityReading, BigDecimal finalWaterReading,
                          BigDecimal outstandingBalance, BigDecimal depositHeld,
                          BigDecimal depositDeducted, BigDecimal depositRefunded,
                          String checklistNotes, UUID recordedByUserId) {
        this.organization = organization;
        this.contract = contract;
        this.terminatedDate = terminatedDate;
        this.finalElectricityReading = finalElectricityReading;
        this.finalWaterReading = finalWaterReading;
        this.outstandingBalance = outstandingBalance;
        this.depositHeld = depositHeld;
        this.depositDeducted = depositDeducted;
        this.depositRefunded = depositRefunded;
        this.checklistNotes = checklistNotes;
        this.recordedByUserId = recordedByUserId;
    }

    public UUID getId() { return id; }
    public UUID getContractId() { return contract.getId(); }
    public LocalDate getTerminatedDate() { return terminatedDate; }
    public BigDecimal getFinalElectricityReading() { return finalElectricityReading; }
    public BigDecimal getFinalWaterReading() { return finalWaterReading; }
    public BigDecimal getOutstandingBalance() { return outstandingBalance; }
    public BigDecimal getDepositHeld() { return depositHeld; }
    public BigDecimal getDepositDeducted() { return depositDeducted; }
    public BigDecimal getDepositRefunded() { return depositRefunded; }
    public String getChecklistNotes() { return checklistNotes; }
    public Instant getCreatedAt() { return createdAt; }
}
