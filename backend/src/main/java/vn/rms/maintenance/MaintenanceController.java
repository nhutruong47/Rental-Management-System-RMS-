package vn.rms.maintenance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.rms.identity.RmsUserPrincipal;

@RestController
@RequestMapping("/api/maintenance-tickets")
public class MaintenanceController {
    private final MaintenanceService maintenance;

    public MaintenanceController(MaintenanceService maintenance) {
        this.maintenance = maintenance;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
    public List<MaintenanceService.TicketResponse> list(@AuthenticationPrincipal RmsUserPrincipal principal) {
        return maintenance.list(principal.organizationId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
    public MaintenanceService.TicketResponse create(@AuthenticationPrincipal RmsUserPrincipal principal,
                                                    @Valid @RequestBody CreateTicketRequest request) {
        return maintenance.create(principal.organizationId(), principal.userId(), request.roomId(),
                request.title(), request.description(), request.priority(), request.estimatedCost());
    }

    @PostMapping("/{ticketId}/assign")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
    public MaintenanceService.TicketResponse assign(@AuthenticationPrincipal RmsUserPrincipal principal,
                                                    @PathVariable UUID ticketId,
                                                    @Valid @RequestBody AssignTicketRequest request) {
        return maintenance.assign(principal.organizationId(), ticketId, request.assigneeName());
    }

    @PostMapping("/{ticketId}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
    public MaintenanceService.TicketResponse transition(@AuthenticationPrincipal RmsUserPrincipal principal,
                                                        @PathVariable UUID ticketId,
                                                        @Valid @RequestBody UpdateTicketStatusRequest request) {
        return maintenance.transition(principal.organizationId(), ticketId, request.status(),
                request.actualCost(), request.resolutionNotes());
    }

    public record CreateTicketRequest(@NotNull UUID roomId,
                                      @NotBlank @Size(max = 160) String title,
                                      @NotBlank @Size(max = 1000) String description,
                                      @NotNull TicketPriority priority,
                                      @DecimalMin("0") BigDecimal estimatedCost) { }

    public record AssignTicketRequest(@NotBlank @Size(max = 120) String assigneeName) { }

    public record UpdateTicketStatusRequest(@NotNull TicketStatus status,
                                            @DecimalMin("0") BigDecimal actualCost,
                                            @Size(max = 1000) String resolutionNotes) { }
}
