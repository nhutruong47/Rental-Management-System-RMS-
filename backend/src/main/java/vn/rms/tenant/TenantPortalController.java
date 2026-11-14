package vn.rms.tenant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import vn.rms.billing.BillingService;
import vn.rms.billing.InvoiceRepository;
import vn.rms.contract.ContractRepository;
import vn.rms.contract.ContractStatus;
import vn.rms.contract.RentalContract;
import vn.rms.identity.RmsUserPrincipal;
import vn.rms.maintenance.MaintenanceService;
import vn.rms.maintenance.MaintenanceTicketRepository;
import vn.rms.maintenance.TicketPriority;
import vn.rms.property.Room;
import vn.rms.property.RoomRepository;

@RestController
@RequestMapping("/api/tenant-portal")
@PreAuthorize("hasRole('TENANT')")
@Transactional(readOnly = true)
public class TenantPortalController {
    private final TenantRepository tenants;
    private final ContractRepository contracts;
    private final RoomRepository rooms;
    private final InvoiceRepository invoices;
    private final MaintenanceTicketRepository tickets;
    private final MaintenanceService maintenance;

    public TenantPortalController(TenantRepository tenants, ContractRepository contracts,
                                  RoomRepository rooms, InvoiceRepository invoices,
                                  MaintenanceTicketRepository tickets, MaintenanceService maintenance) {
        this.tenants = tenants;
        this.contracts = contracts;
        this.rooms = rooms;
        this.invoices = invoices;
        this.tickets = tickets;
        this.maintenance = maintenance;
    }

    @GetMapping("/overview")
    public PortalOverview overview(@AuthenticationPrincipal RmsUserPrincipal principal) {
        Tenant tenant = requireTenant(principal);
        List<RentalContract> myContracts = contracts.findAllByOrganization_IdAndTenant_IdOrderByStartDateDesc(
                principal.organizationId(), tenant.getId());
        Set<UUID> roomIds = myContracts.stream().map(RentalContract::getRoomId).collect(Collectors.toSet());
        List<PortalRoom> myRooms = roomIds.stream()
                .map(roomId -> rooms.findByProperty_Organization_IdAndId(principal.organizationId(), roomId).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(PortalRoom::from)
                .toList();
        List<PortalContract> contractDtos = myContracts.stream().map(c -> {
            String roomCode = myRooms.stream().filter(r -> r.id().equals(c.getRoomId()))
                    .map(PortalRoom::roomCode).findFirst().orElse("—");
            return new PortalContract(c.getId(), c.getRoomId(), roomCode, c.getStartDate(),
                    c.getEndDate(), c.getMonthlyRent(), c.getDepositAmount(), c.getStatus());
        }).toList();
        return new PortalOverview(tenant.getId(), tenant.getFullName(), tenant.getPhone(),
                tenant.getEmail(), myRooms, contractDtos);
    }

    @GetMapping("/invoices")
    public List<BillingService.InvoiceResponse> myInvoices(@AuthenticationPrincipal RmsUserPrincipal principal) {
        Tenant tenant = requireTenant(principal);
        return invoices.findAllByOrganization_IdAndContract_Tenant_IdOrderByBillingPeriodDesc(
                principal.organizationId(), tenant.getId())
                .stream().map(BillingService.InvoiceResponse::from).toList();
    }

    @GetMapping("/tickets")
    public List<MaintenanceService.TicketResponse> myTickets(@AuthenticationPrincipal RmsUserPrincipal principal) {
        Tenant tenant = requireTenant(principal);
        Set<UUID> myRoomIds = contracts.findAllByOrganization_IdAndTenant_IdOrderByStartDateDesc(
                        principal.organizationId(), tenant.getId())
                .stream().map(RentalContract::getRoomId).collect(Collectors.toSet());
        if (myRoomIds.isEmpty()) return List.of();
        return tickets.findAllByOrganization_IdAndRoom_IdInOrderByCreatedAtDesc(
                principal.organizationId(), myRoomIds)
                .stream().map(MaintenanceService.TicketResponse::from).toList();
    }

    @PostMapping("/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public MaintenanceService.TicketResponse createTicket(@AuthenticationPrincipal RmsUserPrincipal principal,
                                                          @Valid @RequestBody TenantTicketRequest request) {
        Tenant tenant = requireTenant(principal);
        boolean activeInRoom = contracts.findAllByOrganization_IdAndTenant_IdOrderByStartDateDesc(
                        principal.organizationId(), tenant.getId())
                .stream().anyMatch(c -> c.getRoomId().equals(request.roomId()) && c.getStatus() == ContractStatus.ACTIVE);
        if (!activeInRoom) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only submit maintenance requests for your active contracted room");
        }
        return maintenance.create(principal.organizationId(), principal.userId(), request.roomId(),
                request.title(), request.description(), request.priority(), BigDecimal.ZERO);
    }

    private Tenant requireTenant(RmsUserPrincipal principal) {
        return tenants.findByOrganizationIdAndUserId(principal.organizationId(), principal.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant profile not linked"));
    }

    public record TenantTicketRequest(@NotNull UUID roomId,
                                      @NotBlank @Size(max = 160) String title,
                                      @NotBlank @Size(max = 1000) String description,
                                      @NotNull TicketPriority priority) { }

    public record PortalRoom(UUID id, String roomCode, BigDecimal areaSquareMeters, BigDecimal monthlyRent) {
        static PortalRoom from(Room room) {
            return new PortalRoom(room.getId(), room.getRoomCode(), room.getAreaSquareMeters(), room.getMonthlyRent());
        }
    }

    public record PortalContract(UUID id, UUID roomId, String roomCode, LocalDate startDate, LocalDate endDate,
                                 BigDecimal monthlyRent, BigDecimal depositAmount, ContractStatus status) { }

    public record PortalOverview(UUID tenantId, String fullName, String phone, String email,
                                 List<PortalRoom> rooms, List<PortalContract> contracts) { }
}
