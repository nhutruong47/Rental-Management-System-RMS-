package vn.rms.billing;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.rms.contract.ContractRepository;
import vn.rms.contract.ContractStatus;
import vn.rms.identity.OrganizationRepository;
import vn.rms.notification.NotificationService;
import vn.rms.utility.MeterReading;
import vn.rms.utility.MeterReadingRepository;
import vn.rms.utility.UtilityType;

@Service
@Transactional
public class BillingService {
    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final ContractRepository contracts;
    private final MeterReadingRepository readings;
    private final OrganizationRepository organizations;
    private final NotificationService notifications;

    public BillingService(InvoiceRepository invoices, PaymentRepository payments, ContractRepository contracts,
                          MeterReadingRepository readings, OrganizationRepository organizations,
                          NotificationService notifications) {
        this.invoices = invoices;
        this.payments = payments;
        this.contracts = contracts;
        this.readings = readings;
        this.organizations = organizations;
        this.notifications = notifications;
    }

    public List<RentalInvoice> generate(UUID organizationId, YearMonth period) {
        List<MeterReading> periodReadings = readings.findByOrganization_IdAndBillingPeriodOrderByRoom_IdAsc(
                organizationId, period.toString());
        var activeContracts = contracts.findByOrganization_IdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                organizationId, ContractStatus.ACTIVE, period.atEndOfMonth(), period.atDay(1));
        var organization = organizations.getReferenceById(organizationId);
        for (var contract : activeContracts) {
            if (invoices.existsByOrganization_IdAndContract_IdAndBillingPeriod(organizationId, contract.getId(), period.toString())) {
                continue;
            }
            BigDecimal electricity = periodReadings.stream()
                    .filter(reading -> reading.getRoomId().equals(contract.getRoomId()) && reading.getUtilityType() == UtilityType.ELECTRICITY)
                    .map(MeterReading::getCharge).findFirst().orElse(BigDecimal.ZERO);
            BigDecimal water = periodReadings.stream()
                    .filter(reading -> reading.getRoomId().equals(contract.getRoomId()) && reading.getUtilityType() == UtilityType.WATER)
                    .map(MeterReading::getCharge).findFirst().orElse(BigDecimal.ZERO);
            invoices.save(new RentalInvoice(organization, contract, period, contract.getMonthlyRent(), electricity, water));
        }
        return invoices.findByOrganization_IdAndBillingPeriodOrderByIdAsc(organizationId, period.toString());
    }

    public RentalInvoice issue(UUID organizationId, UUID invoiceId) {
        RentalInvoice invoice = findInvoice(organizationId, invoiceId);
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only draft invoices can be issued");
        }
        invoice.issue();
        notifications.record(organizationId, "INVOICE_ISSUED",
                "Đã phát hành hóa đơn",
                "Kỳ " + invoice.getBillingPeriod() + " · Tổng thu " + invoice.getTotalAmount() + " VND");
        return invoice;
    }

    public PaymentResult recordPayment(UUID organizationId, UUID userId, UUID invoiceId, BigDecimal amount,
                                       PaymentMethod method, String reference, String idempotencyKey) {
        if (amount.signum() <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment must be positive");
        ManualPayment existing = payments.findByOrganization_IdAndIdempotencyKey(organizationId, idempotencyKey)
                .orElse(null);
        if (existing != null) {
            RentalInvoice existingInvoice = findInvoice(organizationId, invoiceId);
            if (!existingInvoice.getId().equals(existing.getInvoiceId()) || existing.getAmount().compareTo(amount) != 0
                    || existing.getMethod() != method || !existing.getReference().equals(reference)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key already used with different payment details");
            }
            return PaymentResult.from(existing, existingInvoice);
        }
        RentalInvoice invoice = findInvoice(organizationId, invoiceId);
        if (invoice.getStatus() != InvoiceStatus.ISSUED && invoice.getStatus() != InvoiceStatus.PARTIALLY_PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invoice is not payable");
        }
        try {
            invoice.applyPayment(amount);
            ManualPayment payment = payments.saveAndFlush(new ManualPayment(organizations.getReferenceById(organizationId),
                    invoice, amount, method, reference, idempotencyKey, userId));
            notifications.record(organizationId, "PAYMENT_RECORDED",
                    "Ghi nhận thanh toán",
                    "Kỳ " + invoice.getBillingPeriod() + " · +" + amount + " VND (" + reference + ")");
            return PaymentResult.from(payment, invoice);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate payment request");
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    private RentalInvoice findInvoice(UUID organizationId, UUID invoiceId) {
        return invoices.findByOrganization_IdAndId(organizationId, invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
    }

    public List<InvoiceResponse> generateDtos(UUID organizationId, YearMonth period) {
        return generate(organizationId, period).stream().map(InvoiceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listDtos(UUID organizationId, YearMonth period) {
        return invoices.findByOrganization_IdAndBillingPeriodOrderByIdAsc(organizationId, period.toString())
                .stream().map(InvoiceResponse::from).toList();
    }

    public InvoiceResponse issueDto(UUID organizationId, UUID invoiceId) {
        return InvoiceResponse.from(issue(organizationId, invoiceId));
    }

    public record InvoiceResponse(UUID id, UUID contractId, UUID roomId, UUID tenantId, String billingPeriod,
                                  BigDecimal rentAmount, BigDecimal electricityAmount, BigDecimal waterAmount,
                                  BigDecimal totalAmount, BigDecimal amountPaid, BigDecimal balanceDue,
                                  InvoiceStatus status) {
        public static InvoiceResponse from(RentalInvoice invoice) {
            return new InvoiceResponse(invoice.getId(), invoice.getContractId(), invoice.getRoomId(), invoice.getTenantId(),
                    invoice.getBillingPeriod(), invoice.getRentAmount(), invoice.getElectricityAmount(), invoice.getWaterAmount(),
                    invoice.getTotalAmount(), invoice.getAmountPaid(), invoice.getBalanceDue(), invoice.getStatus());
        }
    }

    public record PaymentResult(UUID paymentId, BigDecimal amount, String reference, InvoiceStatus invoiceStatus,
                                BigDecimal balanceDue) {
        static PaymentResult from(ManualPayment payment, RentalInvoice invoice) {
            return new PaymentResult(payment.getId(), payment.getAmount(), payment.getReference(), invoice.getStatus(), invoice.getBalanceDue());
        }
    }
}
