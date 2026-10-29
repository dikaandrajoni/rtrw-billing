package id.rtrwnet.billing;

import id.rtrwnet.billing.domain.Customer;
import id.rtrwnet.billing.domain.CustomerStatus;
import id.rtrwnet.billing.domain.Invoice;
import id.rtrwnet.billing.domain.InvoiceStatus;
import id.rtrwnet.billing.domain.Plan;
import id.rtrwnet.billing.domain.Router;
import id.rtrwnet.billing.domain.RouterStatus;
import id.rtrwnet.billing.domain.Ticket;
import id.rtrwnet.billing.mikrotik.FakeMikrotikGateway;
import id.rtrwnet.billing.repository.CustomerRepository;
import id.rtrwnet.billing.repository.InvoiceRepository;
import id.rtrwnet.billing.repository.PlanRepository;
import id.rtrwnet.billing.repository.RouterRepository;
import id.rtrwnet.billing.repository.TicketRepository;
import id.rtrwnet.billing.service.BillingService;
import id.rtrwnet.billing.service.BusinessException;
import id.rtrwnet.billing.service.CustomerService;
import id.rtrwnet.billing.service.MonitoringService;
import id.rtrwnet.billing.service.PlanService;
import id.rtrwnet.billing.service.RouterService;
import id.rtrwnet.billing.service.TicketService;
import id.rtrwnet.billing.web.dto.CustomerRequest;
import id.rtrwnet.billing.web.dto.PlanRequest;
import id.rtrwnet.billing.web.dto.RouterRequest;
import id.rtrwnet.billing.web.dto.TicketRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Uji alur utama dengan database H2 dan router tiruan.
 * "Hari ini" dikunci ke 15 November 2026 supaya hasil jatuh tempo dan isolir pasti.
 */
@SpringBootTest
class BillingFlowTest {

    static final ZoneId WIB = ZoneId.of("Asia/Jakarta");
    static final YearMonth NOV_2026 = YearMonth.of(2026, 11);

    @TestConfiguration
    static class FixedClock {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(ZonedDateTime.of(2026, 11, 15, 8, 0, 0, 0, WIB).toInstant(), WIB);
        }
    }

    @Autowired PlanService planService;
    @Autowired RouterService routerService;
    @Autowired CustomerService customerService;
    @Autowired BillingService billingService;
    @Autowired TicketService ticketService;
    @Autowired MonitoringService monitoringService;
    @Autowired FakeMikrotikGateway mikrotik;

    @Autowired TicketRepository ticketRepository;
    @Autowired InvoiceRepository invoiceRepository;
    @Autowired CustomerRepository customerRepository;
    @Autowired RouterRepository routerRepository;
    @Autowired PlanRepository planRepository;

    Plan plan10;
    Plan plan20;
    Router router;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        invoiceRepository.deleteAll();
        customerRepository.deleteAll();
        routerRepository.deleteAll();
        planRepository.deleteAll();

        plan10 = planService.create(new PlanRequest("Home 10", "10M/10M", new BigDecimal("150000"), "paket-10m", true));
        plan20 = planService.create(new PlanRequest("Home 20", "20M/20M", new BigDecimal("200000"), "paket-20m", true));
        router = routerService.create(new RouterRequest("NAS-1", "10.0.0.1", null, false, "billing", "secret"));
    }

    private Customer register(String username, int dueDay) {
        return customerService.register(new CustomerRequest("Pelanggan " + username, "6281200000000", null,
                username, "pass12345", plan10.getId(), router.getId(), dueDay));
    }

    @Test
    void registerCreatesPppoeUserWithPlanProfile() {
        Customer c = register("budi", 10);

        assertThat(c.getCode()).isEqualTo("PLG-%05d".formatted(c.getId()));
        assertThat(c.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(mikrotik.profileOf(router, "budi")).isEqualTo("paket-10m");
    }

    @Test
    void duplicatePppoeUsernameIsRejected() {
        register("budi", 10);
        assertThatThrownBy(() -> register("budi", 10)).isInstanceOf(BusinessException.class);
    }

    @Test
    void generateInvoicesIsIdempotentAndUsesDueDay() {
        Customer c = register("budi", 10);

        assertThat(billingService.generateInvoices(NOV_2026)).isEqualTo(1);
        assertThat(billingService.generateInvoices(NOV_2026)).isZero();

        List<Invoice> invoices = billingService.findByCustomer(c.getId());
        assertThat(invoices).hasSize(1);
        assertThat(invoices.get(0).getAmount()).isEqualByComparingTo("150000");
        assertThat(invoices.get(0).getDueDate()).isEqualTo(LocalDate.of(2026, 11, 10));
    }

    @Test
    void overdueCustomerIsIsolatedOnlyAfterGracePeriod() {
        Customer overdue = register("budi", 10);  // jatuh tempo 10 Nov + 3 hari < 15 Nov -> isolir
        Customer notYet = register("siti", 14);   // jatuh tempo 14 Nov, masih masa tenggang
        billingService.generateInvoices(NOV_2026);

        assertThat(billingService.isolateOverdueCustomers()).isEqualTo(1);

        assertThat(customerService.get(overdue.getId()).getStatus()).isEqualTo(CustomerStatus.ISOLATED);
        assertThat(mikrotik.profileOf(router, "budi")).isEqualTo("isolir");
        assertThat(customerService.get(notYet.getId()).getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(mikrotik.profileOf(router, "siti")).isEqualTo("paket-10m");
    }

    @Test
    void payingIsolatedCustomerReactivatesWithCurrentPlan() {
        Customer c = register("budi", 10);
        billingService.generateInvoices(NOV_2026);
        billingService.isolateOverdueCustomers();
        // Ganti paket saat diisolir: tetap isolir, paket baru dipakai saat aktif kembali.
        customerService.changePlan(c.getId(), plan20.getId());
        assertThat(mikrotik.profileOf(router, "budi")).isEqualTo("isolir");

        Invoice invoice = billingService.findByCustomer(c.getId()).get(0);
        Invoice paid = billingService.pay(invoice.getId(), "qris");

        assertThat(paid.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(paid.getPaymentMethod()).isEqualTo("QRIS");
        assertThat(customerService.get(c.getId()).getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(mikrotik.profileOf(router, "budi")).isEqualTo("paket-20m");
    }

    @Test
    void invoiceCannotBePaidTwice() {
        Customer c = register("budi", 10);
        billingService.generateInvoices(NOV_2026);
        Long invoiceId = billingService.findByCustomer(c.getId()).get(0).getId();

        billingService.pay(invoiceId, "cash");
        assertThatThrownBy(() -> billingService.pay(invoiceId, "cash")).isInstanceOf(BusinessException.class);
    }

    @Test
    void terminatedCustomerIsRemovedFromRouterAndNotBilled() {
        Customer c = register("budi", 10);
        customerService.terminate(c.getId());

        assertThat(mikrotik.profileOf(router, "budi")).isNull();
        assertThat(billingService.generateInvoices(NOV_2026)).isZero();
    }

    @Test
    void ticketLifecycle() {
        Customer c = register("budi", 10);
        Ticket t = ticketService.open(new TicketRequest(c.getId(), "Internet mati", "Lampu LOS merah",
                Ticket.Category.NO_INTERNET, null));

        assertThat(t.getNumber()).startsWith("TKT-261115-");
        assertThat(t.getPriority()).isEqualTo(Ticket.Priority.HIGH);

        ticketService.assign(t.getId(), "Rudi");
        ticketService.resolve(t.getId(), "Ganti patchcord");
        Ticket closed = ticketService.close(t.getId());

        assertThat(closed.getStatus()).isEqualTo(Ticket.Status.CLOSED);
        assertThat(closed.getAssignee()).isEqualTo("Rudi");
        assertThat(closed.getNotes()).contains("Ganti patchcord");
        assertThat(ticketService.findOpen()).isEmpty();
    }

    @Test
    void monitoringMarksReachableRouterUp() {
        register("budi", 10);
        Router checked = monitoringService.check(router.getId());

        assertThat(checked.getLastStatus()).isEqualTo(RouterStatus.UP);
        assertThat(checked.getActivePppoeSessions()).isEqualTo(1);
        assertThat(checked.getLastCheckedAt()).isNotNull();
    }
}
