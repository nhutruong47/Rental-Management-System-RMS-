package vn.rms.notification;

import java.time.Instant;
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
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "read_flag", nullable = false)
    private boolean readFlag = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Notification() { }

    public Notification(Organization organization, String eventType, String title, String message) {
        this.organization = organization;
        this.eventType = eventType;
        this.title = title;
        this.message = message;
    }

    public void markRead() { this.readFlag = true; }
    public UUID getId() { return id; }
    public String getEventType() { return eventType; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public boolean isRead() { return readFlag; }
    public Instant getCreatedAt() { return createdAt; }
}
