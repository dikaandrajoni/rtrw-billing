package id.rtrwnet.billing.web.dto;

import id.rtrwnet.billing.domain.Invoice;
import id.rtrwnet.billing.domain.InvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record InvoiceResponse(Long id, String number, Long customerId, String customerName,
                              String period, BigDecimal amount, LocalDate dueDate,
                              InvoiceStatus status, Instant paidAt, String paymentMethod) {

    public static InvoiceResponse from(Invoice i) {
        return new InvoiceResponse(i.getId(), i.getNumber(), i.getCustomer().getId(), i.getCustomer().getName(),
                i.getPeriod(), i.getAmount(), i.getDueDate(), i.getStatus(), i.getPaidAt(), i.getPaymentMethod());
    }
}
