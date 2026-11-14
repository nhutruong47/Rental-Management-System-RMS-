package vn.rms.settings;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.rms.audit.AuditService;

@Service
@Transactional
public class SettingsService {
    private final OrganizationSettingsRepository settingsRepo;
    private final AuditService audit;

    public SettingsService(OrganizationSettingsRepository settingsRepo, AuditService audit) {
        this.settingsRepo = settingsRepo;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public SettingsResponse get(UUID organizationId) {
        return settingsRepo.findById(organizationId)
                .map(SettingsResponse::from)
                .orElseGet(() -> new SettingsResponse((short) 5, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    public SettingsResponse update(UUID organizationId, UUID actorId, String actorEmail,
                                   short billingDueDay, BigDecimal internetFee,
                                   BigDecimal garbageFee, BigDecimal parkingFee) {
        OrganizationSettings settings = settingsRepo.findById(organizationId)
                .orElseGet(() -> new OrganizationSettings(organizationId));
        settings.update(billingDueDay, internetFee, garbageFee, parkingFee);
        settingsRepo.save(settings);
        audit.log(organizationId, actorId, actorEmail, "SETTINGS_UPDATED", "ORGANIZATION_SETTINGS",
                organizationId.toString(),
                "Cập nhật phí định kỳ & hạn thu ngày " + billingDueDay);
        return SettingsResponse.from(settings);
    }

    public record SettingsResponse(short billingDueDay, BigDecimal internetFee,
                                   BigDecimal garbageFee, BigDecimal parkingFee) {
        public static SettingsResponse from(OrganizationSettings s) {
            return new SettingsResponse(s.getBillingDueDay(), s.getInternetFee(), s.getGarbageFee(), s.getParkingFee());
        }
    }
}
