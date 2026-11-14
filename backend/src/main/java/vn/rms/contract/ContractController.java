package vn.rms.contract;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/contracts")
public class ContractController {
    private final ContractService contracts;

    public ContractController(ContractService contracts) {
        this.contracts = contracts;
    }

    @GetMapping
    public java.util.List<ContractService.ContractResponse> list(@AuthenticationPrincipal RmsUserPrincipal principal) {
        return contracts.listDtos(principal.organizationId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContractService.ContractResponse create(@AuthenticationPrincipal RmsUserPrincipal principal,
                                                   @Valid @RequestBody CreateContractRequest request) {
        return contracts.createDto(principal.organizationId(), request.roomId(), request.tenantId(),
                request.startDate(), request.endDate());
    }

    @PostMapping("/{contractId}/activate")
    public ContractService.ContractResponse activate(@AuthenticationPrincipal RmsUserPrincipal principal,
                                                     @PathVariable UUID contractId) {
        return contracts.activateDto(principal.organizationId(), contractId);
    }

    @PostMapping("/{contractId}/terminate")
    public ContractService.HandoverResponse terminate(@AuthenticationPrincipal RmsUserPrincipal principal,
                                                      @PathVariable UUID contractId,
                                                      @Valid @RequestBody TerminateContractRequest request) {
        return contracts.terminate(principal.organizationId(), principal.userId(), contractId,
                request.terminatedDate(), request.depositDeducted(), request.finalElectricityReading(),
                request.finalWaterReading(), request.checklistNotes());
    }

    @GetMapping("/{contractId}/handover")
    public ContractService.HandoverResponse handover(@AuthenticationPrincipal RmsUserPrincipal principal,
                                                     @PathVariable UUID contractId) {
        return contracts.getHandover(principal.organizationId(), contractId);
    }

    public record CreateContractRequest(@NotNull UUID roomId, @NotNull UUID tenantId,
                                        @NotNull LocalDate startDate, @NotNull LocalDate endDate) { }

    public record TerminateContractRequest(@NotNull LocalDate terminatedDate,
                                           @DecimalMin("0") BigDecimal depositDeducted,
                                           @DecimalMin("0") BigDecimal finalElectricityReading,
                                           @DecimalMin("0") BigDecimal finalWaterReading,
                                           @Size(max = 1000) String checklistNotes) { }
}
