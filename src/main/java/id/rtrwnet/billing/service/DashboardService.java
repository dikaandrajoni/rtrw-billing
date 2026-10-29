package id.rtrwnet.billing.service;

import id.rtrwnet.billing.domain.CustomerStatus;
import id.rtrwnet.billing.domain.InvoiceStatus;
import id.rtrwnet.billing.domain.RouterStatus;
import id.rtrwnet.billing.repository.CustomerRepository;
import id.rtrwnet.billing.repository.InvoiceRepository;
import id.rtrwnet.billing.repository.RouterRepository;
import id.rtrwnet.billing.repository.TicketRepository;
import id.rtrwnet.billing.web.dto.DashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final TicketRepository ticketRepository;
    private final RouterRepository routerRepository;

    @Transactional(readOnly = true)
    public DashboardResponse summary(YearMonth period) {
        String p = period.toString();
        return new DashboardResponse(
                p,
                customerRepository.countByStatus(CustomerStatus.ACTIVE),
                customerRepository.countByStatus(CustomerStatus.ISOLATED),
                invoiceRepository.countByPeriodAndStatus(p, InvoiceStatus.UNPAID),
                invoiceRepository.sumAmount(p, InvoiceStatus.UNPAID),
                invoiceRepository.countByPeriodAndStatus(p, InvoiceStatus.PAID),
                invoiceRepository.sumAmount(p, InvoiceStatus.PAID),
                ticketRepository.countByStatusIn(TicketService.OPEN_STATUSES),
                routerRepository.countByLastStatus(RouterStatus.DOWN));
    }
}
