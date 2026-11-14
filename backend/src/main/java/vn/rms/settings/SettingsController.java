package vn.rms.settings;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.rms.identity.RmsUserPrincipal;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {
    private final SettingsService settings;

    public SettingsController(SettingsService settings) {
        this.settings = settings;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
    public SettingsService.SettingsResponse get(@AuthenticationPrincipal RmsUserPrincipal principal) {
        return settings.get(principal.organizationId());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public SettingsService.SettingsResponse update(@AuthenticationPrincipal RmsUserPrincipal principal,
                                                   @Valid @RequestBody UpdateSettingsRequest request) {
        return settings.update(principal.organizationId(), principal.userId(), principal.email(),
                request.billingDueDay(), request.internetFee(), request.garbageFee(), request.parkingFee());
    }

    public record UpdateSettingsRequest(@Min(1) @Max(28) short billingDueDay,
                                        @NotNull @DecimalMin("0") BigDecimal internetFee,
                                        @NotNull @DecimalMin("0") BigDecimal garbageFee,
                                        @NotNull @DecimalMin("0") BigDecimal parkingFee) { }
}
