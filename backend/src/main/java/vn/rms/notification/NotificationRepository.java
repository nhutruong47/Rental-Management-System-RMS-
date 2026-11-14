package vn.rms.notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findTop30ByOrganization_IdOrderByCreatedAtDesc(UUID organizationId);
    Optional<Notification> findByOrganization_IdAndId(UUID organizationId, UUID id);
}
