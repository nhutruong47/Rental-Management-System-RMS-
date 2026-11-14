package vn.rms.contract;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.rms.billing.InvoiceRepository;
import vn.rms.billing.InvoiceStatus;
import vn.rms.billing.RentalInvoice;
import vn.rms.notification.NotificationService;
import vn.rms.property.Room;
import vn.rms.property.RoomRepository;
import vn.rms.property.RoomStatus;
import vn.rms.tenant.Tenant;
import vn.rms.tenant.TenantRepository;

@Service
@Transactional
public class ContractService {
    private final ContractRepository contracts;
    private final RoomRepository rooms;
    private final TenantRepository tenants;
    private final InvoiceRepository invoices;
    private final HandoverRecordRepository handovers;
    private final NotificationService notifications;

    public ContractService(ContractRepository contracts, RoomRepository rooms, TenantRepository tenants,
                           InvoiceRepository invoices, HandoverRecordRepository handovers,
                           NotificationService notifications) {
        this.contracts = contracts;
        this.rooms = rooms;
        this.tenants = tenants;
        this.invoices = invoices;
        this.handovers = handovers;
        this.notifications = notifications;
    }

    public RentalContract create(UUID organizationId, UUID roomId, UUID tenantId,
                                 LocalDate start, LocalDate end) {
        if (end.isBefore(start)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid contract period");
        Room room = rooms.findByProperty_Organization_IdAndId(organizationId, roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        Tenant tenant = tenants.findByOrganizationIdAndId(organizationId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
        RentalContract contract = new RentalContract(room.getOrganization(), room, tenant, start, end,
                room.getMonthlyRent(), room.getDepositAmount());
        return contracts.save(contract);
    }

    public RentalContract activate(UUID organizationId, UUID contractId) {
        RentalContract contract = contracts.findByOrganization_IdAndId(organizationId, contractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract not found"));
        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only draft contracts can be activated");
        }
        if (contracts.existsByRoom_IdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                contract.getRoomId(), ContractStatus.ACTIVE, contract.getEndDate(), contract.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room has an overlapping active contract");
        }
        Room room = rooms.findByProperty_Organization_IdAndId(organizationId, contract.getRoomId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is not available");
        }
        room.markOccupied();
        contract.activate();
        return contract;
    }

    public HandoverResponse terminate(UUID organizationId, UUID userId, UUID contractId, LocalDate terminatedDate,
                                      BigDecimal depositDeducted, BigDecimal finalElectricityReading,
                                      BigDecimal finalWaterReading, String checklistNotes) {
        RentalContract contract = contracts.findByOrganization_IdAndId(organizationId, contractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract not found"));
        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only active contracts can be terminated");
        }
        if (terminatedDate.isBefore(contract.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Termination date cannot precede contract start date");
        }
        var contractInvoices = invoices.findByOrganization_IdAndContract_Id(organizationId, contractId);
        if (contractInvoices.stream().anyMatch(inv -> inv.getStatus() == InvoiceStatus.DRAFT)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Draft invoices must be issued and settled before termination");
        }
        BigDecimal outstanding = contractInvoices.stream()
                .filter(inv -> inv.getStatus() != InvoiceStatus.VOID)
                .map(RentalInvoice::getBalanceDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (outstanding.signum() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Outstanding invoice balance must be settled before termination");
        }
        BigDecimal depositHeld = contract.getDepositAmount();
        BigDecimal deducted = depositDeducted == null ? BigDecimal.ZERO : depositDeducted;
        if (deducted.signum() < 0 || deducted.compareTo(depositHeld) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deposit deduction must be between 0 and held deposit");
        }
        BigDecimal refunded = depositHeld.subtract(deducted);
        Room room = rooms.findByProperty_Organization_IdAndId(organizationId, contract.getRoomId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        room.releaseToAvailable();
        contract.terminate();
        HandoverRecord handover = handovers.save(new HandoverRecord(
                contract.getOrganization(), contract, terminatedDate,
                finalElectricityReading, finalWaterReading,
                outstanding, depositHeld, deducted, refunded,
                checklistNotes == null ? null : checklistNotes.trim(), userId));
        notifications.record(organizationId, "CONTRACT_TERMINATED",
                "Thanh lý hợp đồng & trả phòng",
                "Phòng " + room.getRoomCode() + " đã bàn giao và chuyển về trạng thái Trống.");
        return HandoverResponse.from(handover);
    }

    @Transactional(readOnly = true)
    public HandoverResponse getHandover(UUID organizationId, UUID contractId) {
        return handovers.findByOrganization_IdAndContract_Id(organizationId, contractId)
                .map(HandoverResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Handover record not found"));
    }

    @Transactional(readOnly = true)
    public java.util.List<RentalContract> list(UUID organizationId) {
        return contracts.findAllByOrganization_IdOrderByStartDateDesc(organizationId);
    }

    public ContractResponse createDto(UUID organizationId, UUID roomId, UUID tenantId, LocalDate start, LocalDate end) {
        return ContractResponse.from(create(organizationId, roomId, tenantId, start, end));
    }

    public ContractResponse activateDto(UUID organizationId, UUID contractId) {
        return ContractResponse.from(activate(organizationId, contractId));
    }

    @Transactional(readOnly = true)
    public java.util.List<ContractResponse> listDtos(UUID organizationId) {
        return list(organizationId).stream().map(ContractResponse::from).toList();
    }

    public record ContractResponse(UUID id, UUID roomId, UUID tenantId, LocalDate startDate, LocalDate endDate,
                                   BigDecimal monthlyRent, BigDecimal depositAmount, ContractStatus status) {
        public static ContractResponse from(RentalContract contract) {
            return new ContractResponse(contract.getId(), contract.getRoomId(), contract.getTenantId(),
                    contract.getStartDate(), contract.getEndDate(), contract.getMonthlyRent(),
                    contract.getDepositAmount(), contract.getStatus());
        }
    }

    public record HandoverResponse(UUID id, UUID contractId, LocalDate terminatedDate,
                                   BigDecimal finalElectricityReading, BigDecimal finalWaterReading,
                                   BigDecimal outstandingBalance, BigDecimal depositHeld,
                                   BigDecimal depositDeducted, BigDecimal depositRefunded,
                                   String checklistNotes, Instant createdAt) {
        public static HandoverResponse from(HandoverRecord record) {
            return new HandoverResponse(record.getId(), record.getContractId(), record.getTerminatedDate(),
                    record.getFinalElectricityReading(), record.getFinalWaterReading(),
                    record.getOutstandingBalance(), record.getDepositHeld(),
                    record.getDepositDeducted(), record.getDepositRefunded(),
                    record.getChecklistNotes(), record.getCreatedAt());
        }
    }

    public BigDecimal monthlyRent(RentalContract contract) { return contract.getMonthlyRent(); }
}
