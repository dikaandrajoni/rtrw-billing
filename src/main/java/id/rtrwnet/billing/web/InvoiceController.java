package id.rtrwnet.billing.web;

import id.rtrwnet.billing.service.BillingService;
import id.rtrwnet.billing.web.dto.InvoiceResponse;
import id.rtrwnet.billing.web.dto.PaymentRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final BillingService billingService;
    private final Clock clock;

    /** Contoh: GET /api/invoices?period=2026-11 (default bulan ini). */
    @GetMapping
    public List<InvoiceResponse> list(@RequestParam(required = false) YearMonth period) {
        YearMonth p = period != null ? period : YearMonth.now(clock);
        return billingService.findByPeriod(p).stream().map(InvoiceResponse::from).toList();
    }

    @GetMapping("/{id}")
    public InvoiceResponse get(@PathVariable Long id) {
        return InvoiceResponse.from(billingService.getInvoice(id));
    }

    /** Terbitkan tagihan secara manual (biasanya otomatis tiap tanggal 1). */
    @PostMapping("/generate")
    public Map<String, Object> generate(@RequestParam(required = false) YearMonth period) {
        YearMonth p = period != null ? period : YearMonth.now(clock);
        return Map.of("period", p.toString(), "created", billingService.generateInvoices(p));
    }

    @PostMapping("/{id}/pay")
    public InvoiceResponse pay(@PathVariable Long id, @Valid @RequestBody PaymentRequest request) {
        return InvoiceResponse.from(billingService.pay(id, request.method()));
    }

    @PostMapping("/{id}/cancel")
    public InvoiceResponse cancel(@PathVariable Long id) {
        return InvoiceResponse.from(billingService.cancel(id));
    }

    /** Jalankan isolir otomatis sekarang (biasanya terjadwal tiap malam). */
    @PostMapping("/isolate-overdue")
    public Map<String, Object> isolateOverdue() {
        return Map.of("isolated", billingService.isolateOverdueCustomers());
    }
}
