package vn.rms.notification;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.rms.identity.RmsUserPrincipal;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public List<NotificationService.NotificationResponse> list(@AuthenticationPrincipal RmsUserPrincipal principal) {
        return notifications.list(principal.organizationId());
    }

    @PostMapping("/{notificationId}/read")
    public NotificationService.NotificationResponse markRead(@AuthenticationPrincipal RmsUserPrincipal principal,
                                                           @PathVariable UUID notificationId) {
        return notifications.markRead(principal.organizationId(), notificationId);
    }
}
