package id.rtrwnet.billing.web.dto;

import java.math.BigDecimal;

public record DashboardResponse(
        String period,
        long activeCustomers,
        long isolatedCustomers,
        long invoicesUnpaid,
        BigDecimal amountUnpaid,
        long invoicesPaid,
        BigDecimal amountPaid,
        long openTickets,
        long routersDown
) {
}
