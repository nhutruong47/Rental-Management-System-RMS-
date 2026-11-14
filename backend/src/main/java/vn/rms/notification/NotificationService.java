package vn.rms.notification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.rms.identity.OrganizationRepository;

@Service
@Transactional
public class NotificationService {
    private final NotificationRepository notifications;
    private final OrganizationRepository organizations;

    public NotificationService(NotificationRepository notifications, OrganizationRepository organizations) {
        this.notifications = notifications;
        this.organizations = organizations;
    }

    public void record(UUID organizationId, String eventType, String title, String message) {
        notifications.save(new Notification(organizations.getReferenceById(organizationId), eventType, title, message));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(UUID organizationId) {
        return notifications.findTop30ByOrganization_IdOrderByCreatedAtDesc(organizationId)
                .stream().map(NotificationResponse::from).toList();
    }

    public NotificationResponse markRead(UUID organizationId, UUID id) {
        Notification notification = notifications.findByOrganization_IdAndId(organizationId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        notification.markRead();
        return NotificationResponse.from(notification);
    }

    public record NotificationResponse(UUID id, String eventType, String title, String message, boolean read, Instant createdAt) {
        public static NotificationResponse from(Notification n) {
            return new NotificationResponse(n.getId(), n.getEventType(), n.getTitle(), n.getMessage(), n.isRead(), n.getCreatedAt());
        }
    }
}
