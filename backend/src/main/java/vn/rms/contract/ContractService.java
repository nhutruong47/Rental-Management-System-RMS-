package vn.rms.contract;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.rms.property.Room;
import vn.rms.property.RoomStatus;
import vn.rms.property.RoomRepository;
import vn.rms.tenant.Tenant;
import vn.rms.tenant.TenantRepository;

@Service
@Transactional
public class ContractService {
    private final ContractRepository contracts;
    private final RoomRepository rooms;
    private final TenantRepository tenants;

    public ContractService(ContractRepository contracts, RoomRepository rooms, TenantRepository tenants) {
        this.contracts = contracts;
        this.rooms = rooms;
        this.tenants = tenants;
    }

    public RentalContract create(UUID organizationId, UUID roomId, UUID tenantId,
                                 LocalDate start, LocalDate end) {
        if (end.isBefore(start)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid contract period");
        Room room = rooms.findByProperty_Organization_IdAndId(organizationId, roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        Tenant tenant = tenants.findByOrganizationIdAndId(organizationId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
        RentalContract contract = new RentalContract(room.getOrganization(), room, tenant, start, end, room.getMonthlyRent());
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
                                   BigDecimal monthlyRent, ContractStatus status) {
        public static ContractResponse from(RentalContract contract) {
            return new ContractResponse(contract.getId(), contract.getRoomId(), contract.getTenantId(),
                    contract.getStartDate(), contract.getEndDate(), contract.getMonthlyRent(), contract.getStatus());
        }
    }

    public BigDecimal monthlyRent(RentalContract contract) { return contract.getMonthlyRent(); }
}