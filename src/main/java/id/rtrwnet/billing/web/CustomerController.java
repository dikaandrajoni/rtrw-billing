package id.rtrwnet.billing.web;

import id.rtrwnet.billing.domain.CustomerStatus;
import id.rtrwnet.billing.service.BillingService;
import id.rtrwnet.billing.service.CustomerService;
import id.rtrwnet.billing.service.TicketService;
import id.rtrwnet.billing.web.dto.ChangePlanRequest;
import id.rtrwnet.billing.web.dto.CustomerRequest;
import id.rtrwnet.billing.web.dto.CustomerResponse;
import id.rtrwnet.billing.web.dto.InvoiceResponse;
import id.rtrwnet.billing.web.dto.TicketResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final BillingService billingService;
    private final TicketService ticketService;

    @GetMapping
    public Page<CustomerResponse> search(@RequestParam(required = false) String q,
                                         @RequestParam(required = false) CustomerStatus status,
                                         @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return customerService.search(q, status, pageable).map(CustomerResponse::from);
    }

    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable Long id) {
        return CustomerResponse.from(customerService.get(id));
    }

    /** Daftarkan pelanggan baru sekaligus membuat user PPPoE di MikroTik. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse register(@Valid @RequestBody CustomerRequest request) {
        return CustomerResponse.from(customerService.register(request));
    }

    @PutMapping("/{id}/plan")
    public CustomerResponse changePlan(@PathVariable Long id, @Valid @RequestBody ChangePlanRequest request) {
        return CustomerResponse.from(customerService.changePlan(id, request.planId()));
    }

    @PostMapping("/{id}/isolate")
    public CustomerResponse isolate(@PathVariable Long id) {
        return CustomerResponse.from(customerService.isolate(id));
    }

    @PostMapping("/{id}/reactivate")
    public CustomerResponse reactivate(@PathVariable Long id) {
        return CustomerResponse.from(customerService.reactivate(id));
    }

    @PostMapping("/{id}/terminate")
    public CustomerResponse terminate(@PathVariable Long id) {
        return CustomerResponse.from(customerService.terminate(id));
    }

    @GetMapping("/{id}/invoices")
    public List<InvoiceResponse> invoices(@PathVariable Long id) {
        return billingService.findByCustomer(id).stream().map(InvoiceResponse::from).toList();
    }

    @GetMapping("/{id}/tickets")
    public List<TicketResponse> tickets(@PathVariable Long id) {
        return ticketService.findByCustomer(id).stream().map(TicketResponse::from).toList();
    }
}
