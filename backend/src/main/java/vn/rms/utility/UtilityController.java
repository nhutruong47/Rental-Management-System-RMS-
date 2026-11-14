package vn.rms.utility;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.rms.identity.RmsUserPrincipal;

@RestController
public class UtilityController {
    private final UtilityService utilities;

    public UtilityController(UtilityService utilities) {
        this.utilities = utilities;
    }

    @GetMapping("/api/utility-tariffs")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
    public List<UtilityService.TariffResponse> listTariffs(@AuthenticationPrincipal RmsUserPrincipal principal) {
        return utilities.listTariffDtos(principal.organizationId());
    }

    @PostMapping("/api/utility-tariffs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public UtilityService.TariffResponse createTariff(@AuthenticationPrincipal RmsUserPrincipal principal,
                                                      @Valid @RequestBody CreateTariffRequest request) {
        return utilities.createTariffDto(principal.organizationId(), request.utilityType(),
                request.pricePerUnit(), request.effectiveFrom());
    }

    @PostMapping("/api/meter-readings")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
    public UtilityService.MeterReadingResponse createReading(@AuthenticationPrincipal RmsUserPrincipal principal,
                                                             @Valid @RequestBody CreateReadingRequest request) {
        return utilities.createReadingDto(principal.organizationId(), request.roomId(),
                request.utilityType(), request.billingPeriod(), request.currentReading());
    }

    @GetMapping("/api/meter-readings")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
    public List<UtilityService.MeterReadingResponse> readings(@AuthenticationPrincipal RmsUserPrincipal principal,
                                                              @org.springframework.web.bind.annotation.RequestParam
                                                              @DateTimeFormat(pattern = "yyyy-MM") YearMonth billingPeriod) {
        return utilities.readingDtos(principal.organizationId(), billingPeriod);
    }

    public record CreateTariffRequest(@NotNull UtilityType utilityType,
                                      @NotNull @DecimalMin("0") BigDecimal pricePerUnit,
                                      @NotNull LocalDate effectiveFrom) { }
    public record CreateReadingRequest(@NotNull UUID roomId, @NotNull UtilityType utilityType,
                                       @NotNull @DateTimeFormat(pattern = "yyyy-MM") YearMonth billingPeriod,
                                       @NotNull @DecimalMin("0") BigDecimal currentReading) { }
}
