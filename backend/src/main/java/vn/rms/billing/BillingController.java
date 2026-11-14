package vn.rms.billing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/invoices")
public class BillingController {
    private final BillingService billing;

    public BillingController(BillingService billing) {
        this.billing = billing;
    }

    @PostMapping("/generate")
    public List<BillingService.InvoiceResponse> generate(@AuthenticationPrincipal RmsUserPrincipal principal,
                                                         @Valid @RequestBody GenerateRequest request) {
        return billing.generateDtos(principal.organizationId(), request.billingPeriod());
    }

    @GetMapping
    public List<BillingService.InvoiceResponse> list(@AuthenticationPrincipal RmsUserPrincipal principal,
                                                     @org.springframework.web.bind.annotation.RequestParam
                                                     @DateTimeFormat(pattern = "yyyy-MM") YearMonth billingPeriod) {
        return billing.listDtos(principal.organizationId(), billingPeriod);
    }

    @PostMapping("/{invoiceId}/issue")
    public BillingService.InvoiceResponse issue(@AuthenticationPrincipal RmsUserPrincipal principal, @PathVariable UUID invoiceId) {
        return billing.issueDto(principal.organizationId(), invoiceId);
    }

    @PostMapping("/{invoiceId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse payment(@AuthenticationPrincipal RmsUserPrincipal principal, @PathVariable UUID invoiceId,
                                   @Valid @RequestBody RecordPaymentRequest request) {
        return PaymentResponse.from(billing.recordPayment(principal.organizationId(), principal.userId(), invoiceId,
                request.amount(), request.method(), request.reference(), request.idempotencyKey()));
    }

    public record GenerateRequest(@NotNull @DateTimeFormat(pattern = "yyyy-MM") YearMonth billingPeriod) { }
    public record RecordPaymentRequest(@NotNull @DecimalMin(value = "1") BigDecimal amount,
                                       @NotNull PaymentMethod method,
                                       @NotBlank @Size(max = 120) String reference,
                                       @NotBlank @Size(max = 80) String idempotencyKey) { }
    public record PaymentResponse(UUID paymentId, BigDecimal amount, String reference, InvoiceStatus invoiceStatus,
                                  BigDecimal balanceDue) {
        static PaymentResponse from(BillingService.PaymentResult result) {
            return new PaymentResponse(result.paymentId(), result.amount(), result.reference(), result.invoiceStatus(), result.balanceDue());
        }
    }
}
