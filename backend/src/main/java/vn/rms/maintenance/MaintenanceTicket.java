package vn.rms.maintenance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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
import vn.rms.identity.Organization;
import vn.rms.property.Room;

@Entity
@Table(name = "maintenance_tickets")
public class MaintenanceTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status = TicketStatus.OPEN;

    @Column(name = "assignee_name", length = 120)
    private String assigneeName;

    @Column(name = "estimated_cost", nullable = false, precision = 19, scale = 0)
    private BigDecimal estimatedCost = BigDecimal.ZERO;

    @Column(name = "actual_cost", nullable = false, precision = 19, scale = 0)
    private BigDecimal actualCost = BigDecimal.ZERO;

    @Column(name = "resolution_notes", length = 1000)
    private String resolutionNotes;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    @Column(nullable = false)
    private long version;

    protected MaintenanceTicket() { }

    public MaintenanceTicket(Organization organization, Room room, String title, String description,
                             TicketPriority priority, BigDecimal estimatedCost, UUID createdByUserId) {
        this.organization = organization;
        this.room = room;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.estimatedCost = estimatedCost == null ? BigDecimal.ZERO : estimatedCost;
        this.createdByUserId = createdByUserId;
    }

    public void assign(String assigneeName) {
        if (!status.canTransitionTo(TicketStatus.ASSIGNED)) {
            throw new IllegalStateException("Ticket cannot be assigned from status " + status);
        }
        this.assigneeName = assigneeName;
        this.status = TicketStatus.ASSIGNED;
        this.updatedAt = Instant.now();
    }

    public void transitionTo(TicketStatus nextStatus, BigDecimal actualCost, String resolutionNotes) {
        if (!status.canTransitionTo(nextStatus)) {
            throw new IllegalStateException("Invalid ticket status transition from " + status + " to " + nextStatus);
        }
        if (actualCost != null) {
            if (actualCost.signum() < 0) throw new IllegalArgumentException("Actual cost cannot be negative");
            this.actualCost = actualCost;
        }
        if (resolutionNotes != null && !resolutionNotes.isBlank()) {
            this.resolutionNotes = resolutionNotes.trim();
        }
        this.status = nextStatus;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getRoomId() { return room.getId(); }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public TicketPriority getPriority() { return priority; }
    public TicketStatus getStatus() { return status; }
    public String getAssigneeName() { return assigneeName; }
    public BigDecimal getEstimatedCost() { return estimatedCost; }
    public BigDecimal getActualCost() { return actualCost; }
    public String getResolutionNotes() { return resolutionNotes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
