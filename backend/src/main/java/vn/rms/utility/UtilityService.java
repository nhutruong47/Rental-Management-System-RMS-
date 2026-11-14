package vn.rms.utility;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.rms.identity.OrganizationRepository;
import vn.rms.property.Room;
import vn.rms.property.RoomRepository;

@Service
@Transactional
public class UtilityService {
    private final UtilityTariffRepository tariffs;
    private final MeterReadingRepository readings;
    private final RoomRepository rooms;
    private final OrganizationRepository organizations;

    public UtilityService(UtilityTariffRepository tariffs, MeterReadingRepository readings,
                          RoomRepository rooms, OrganizationRepository organizations) {
        this.tariffs = tariffs;
        this.readings = readings;
        this.rooms = rooms;
        this.organizations = organizations;
    }

    @Transactional(readOnly = true)
    public List<UtilityTariff> listTariffs(UUID organizationId) {
        return tariffs.findAllByOrganization_IdOrderByEffectiveFromDesc(organizationId);
    }

    public UtilityTariff createTariff(UUID organizationId, UtilityType type, BigDecimal price, LocalDate effectiveFrom) {
        if (price.signum() < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tariff cannot be negative");
        var organization = organizations.findById(organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
        return tariffs.save(new UtilityTariff(organization, type, price, effectiveFrom));
    }

    public MeterReading createReading(UUID organizationId, UUID roomId, UtilityType type,
                                      YearMonth period, BigDecimal currentReading) {
        String periodText = period.toString();
        Room room = rooms.findByProperty_Organization_IdAndId(organizationId, roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        if (readings.findByOrganization_IdAndRoom_IdAndUtilityTypeAndBillingPeriod(organizationId, roomId, type, periodText).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reading already exists for this room and period");
        }
        BigDecimal previous = readings
                .findFirstByOrganization_IdAndRoom_IdAndUtilityTypeAndBillingPeriodLessThanOrderByBillingPeriodDesc(
                        organizationId, roomId, type, periodText)
                .map(MeterReading::getCurrentReading).orElse(BigDecimal.ZERO);
        if (readings.findFirstByOrganization_IdAndRoom_IdAndUtilityTypeAndBillingPeriodGreaterThanOrderByBillingPeriodAsc(
                organizationId, roomId, type, periodText).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Meter readings must be entered in chronological order");
        }
        if (currentReading.compareTo(previous) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meter reading cannot decrease without meter replacement");
        }
        LocalDate effectiveOn = period.atEndOfMonth();
        BigDecimal price = tariffs
                .findFirstByOrganization_IdAndUtilityTypeAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        organizationId, type, effectiveOn)
                .map(UtilityTariff::getPricePerUnit)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No tariff configured for billing period"));
        BigDecimal charge = currentReading.subtract(previous).multiply(price).setScale(0, RoundingMode.HALF_UP);
        var organization = organizations.getReferenceById(organizationId);
        return readings.save(new MeterReading(organization, room, type, period, previous, currentReading, price, charge));
    }

    @Transactional(readOnly = true)
    public List<MeterReading> readings(UUID organizationId, YearMonth period) {
        return readings.findByOrganization_IdAndBillingPeriodOrderByRoom_IdAsc(organizationId, period.toString());
    }

    @Transactional(readOnly = true)
    public List<TariffResponse> listTariffDtos(UUID organizationId) {
        return listTariffs(organizationId).stream().map(TariffResponse::from).toList();
    }

    public TariffResponse createTariffDto(UUID organizationId, UtilityType type, BigDecimal price, LocalDate effectiveFrom) {
        return TariffResponse.from(createTariff(organizationId, type, price, effectiveFrom));
    }

    public MeterReadingResponse createReadingDto(UUID organizationId, UUID roomId, UtilityType type,
                                                 YearMonth period, BigDecimal currentReading) {
        return MeterReadingResponse.from(createReading(organizationId, roomId, type, period, currentReading));
    }

    @Transactional(readOnly = true)
    public List<MeterReadingResponse> readingDtos(UUID organizationId, YearMonth period) {
        return readings(organizationId, period).stream().map(MeterReadingResponse::from).toList();
    }

    public record TariffResponse(UUID id, UtilityType utilityType, BigDecimal pricePerUnit, LocalDate effectiveFrom) {
        public static TariffResponse from(UtilityTariff tariff) {
            return new TariffResponse(tariff.getId(), tariff.getUtilityType(), tariff.getPricePerUnit(), tariff.getEffectiveFrom());
        }
    }

    public record MeterReadingResponse(UUID id, UUID roomId, UtilityType utilityType, String billingPeriod,
                                       BigDecimal previousReading, BigDecimal currentReading, BigDecimal usage,
                                       BigDecimal unitPrice, BigDecimal charge) {
        public static MeterReadingResponse from(MeterReading reading) {
            return new MeterReadingResponse(reading.getId(), reading.getRoomId(), reading.getUtilityType(),
                    reading.getBillingPeriod(), reading.getPreviousReading(), reading.getCurrentReading(),
                    reading.getUsage(), reading.getUnitPrice(), reading.getCharge());
        }
    }
}
