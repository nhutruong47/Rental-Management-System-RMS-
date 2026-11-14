package vn.rms.property;

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
import org.hibernate.annotations.CreationTimestamp;
import vn.rms.identity.Organization;

@Entity
@Table(name = "rooms")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "room_code", nullable = false, length = 40)
    private String roomCode;

    @Column(name = "area_square_meters", nullable = false, precision = 8, scale = 2)
    private BigDecimal areaSquareMeters;

    @Column(nullable = false)
    private short capacity;

    @Column(name = "monthly_rent", nullable = false, precision = 19, scale = 0)
    private BigDecimal monthlyRent;

    @Column(name = "deposit_amount", nullable = false, precision = 19, scale = 0)
    private BigDecimal depositAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomStatus status = RoomStatus.AVAILABLE;

    @Version
    @Column(nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Room() {
    }

    public Room(Organization organization, Property property, String roomCode, BigDecimal areaSquareMeters,
                short capacity, BigDecimal monthlyRent, BigDecimal depositAmount) {
        this.organization = organization;
        this.property = property;
        this.roomCode = roomCode;
        this.areaSquareMeters = areaSquareMeters;
        this.capacity = capacity;
        this.monthlyRent = monthlyRent;
        this.depositAmount = depositAmount;
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organization.getId(); }
    public Organization getOrganization() { return organization; }
    public UUID getPropertyId() { return property.getId(); }
    public String getRoomCode() { return roomCode; }
    public BigDecimal getAreaSquareMeters() { return areaSquareMeters; }
    public short getCapacity() { return capacity; }
    public BigDecimal getMonthlyRent() { return monthlyRent; }
    public BigDecimal getDepositAmount() { return depositAmount; }
    public RoomStatus getStatus() { return status; }
    public void markOccupied() {
        if (status != RoomStatus.AVAILABLE) throw new IllegalStateException("Only available rooms can be occupied");
        status = RoomStatus.OCCUPIED;
    }

    public void releaseToAvailable() {
        if (status != RoomStatus.OCCUPIED) throw new IllegalStateException("Only occupied rooms can be released");
        status = RoomStatus.AVAILABLE;
    }

    public void setOperationalStatus(RoomStatus target) {
        if (status == RoomStatus.OCCUPIED) {
            throw new IllegalStateException("Cannot manually change status of an occupied room");
        }
        if (target != RoomStatus.AVAILABLE && target != RoomStatus.MAINTENANCE && target != RoomStatus.INACTIVE) {
            throw new IllegalArgumentException("Unsupported manual room status: " + target);
        }
        status = target;
    }
}
