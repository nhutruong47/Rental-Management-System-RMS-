package vn.rms.maintenance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.rms.identity.OrganizationRepository;
import vn.rms.notification.NotificationService;
import vn.rms.property.Room;
import vn.rms.property.RoomRepository;

@Service
@Transactional
public class MaintenanceService {
    private final MaintenanceTicketRepository tickets;
    private final RoomRepository rooms;
    private final OrganizationRepository organizations;
    private final NotificationService notifications;

    public MaintenanceService(MaintenanceTicketRepository tickets, RoomRepository rooms,
                              OrganizationRepository organizations, NotificationService notifications) {
        this.tickets = tickets;
        this.rooms = rooms;
        this.organizations = organizations;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> list(UUID organizationId) {
        return tickets.findAllByOrganization_IdOrderByCreatedAtDesc(organizationId)
                .stream().map(TicketResponse::from).toList();
    }

    public TicketResponse create(UUID organizationId, UUID userId, UUID roomId, String title, String description,
                                 TicketPriority priority, BigDecimal estimatedCost) {
        Room room = rooms.findByProperty_Organization_IdAndId(organizationId, roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        MaintenanceTicket saved = tickets.save(new MaintenanceTicket(
                organizations.getReferenceById(organizationId),
                room, title.trim(), description.trim(), priority, estimatedCost, userId));
        notifications.record(organizationId, "MAINTENANCE_CREATED",
                "Yêu cầu bảo trì mới",
                "Phòng " + room.getRoomCode() + ": " + saved.getTitle());
        return TicketResponse.from(saved);
    }

    public TicketResponse assign(UUID organizationId, UUID ticketId, String assigneeName) {
        MaintenanceTicket ticket = requireTicket(organizationId, ticketId);
        try {
            ticket.assign(assigneeName.trim());
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
        notifications.record(organizationId, "MAINTENANCE_ASSIGNED",
                "Đã phân công bảo trì",
                ticket.getTitle() + " → " + ticket.getAssigneeName());
        return TicketResponse.from(ticket);
    }

    public TicketResponse transition(UUID organizationId, UUID ticketId, TicketStatus nextStatus,
                                     BigDecimal actualCost, String resolutionNotes) {
        MaintenanceTicket ticket = requireTicket(organizationId, ticketId);
        try {
            ticket.transitionTo(nextStatus, actualCost, resolutionNotes);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        notifications.record(organizationId, "MAINTENANCE_UPDATED",
                "Cập nhật trạng thái bảo trì",
                ticket.getTitle() + " → " + nextStatus.name());
        return TicketResponse.from(ticket);
    }

    private MaintenanceTicket requireTicket(UUID organizationId, UUID ticketId) {
        return tickets.findByOrganization_IdAndId(organizationId, ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Maintenance ticket not found"));
    }

    public record TicketResponse(UUID id, UUID roomId, String title, String description,
                                 TicketPriority priority, TicketStatus status, String assigneeName,
                                 BigDecimal estimatedCost, BigDecimal actualCost, String resolutionNotes,
                                 Instant createdAt, Instant updatedAt) {
        public static TicketResponse from(MaintenanceTicket t) {
            return new TicketResponse(t.getId(), t.getRoomId(), t.getTitle(), t.getDescription(),
                    t.getPriority(), t.getStatus(), t.getAssigneeName(), t.getEstimatedCost(),
                    t.getActualCost(), t.getResolutionNotes(), t.getCreatedAt(), t.getUpdatedAt());
        }
    }
}
