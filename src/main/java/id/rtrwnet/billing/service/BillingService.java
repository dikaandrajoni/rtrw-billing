package id.rtrwnet.billing.service;

import id.rtrwnet.billing.config.BillingProperties;
import id.rtrwnet.billing.domain.Customer;
import id.rtrwnet.billing.domain.CustomerStatus;
import id.rtrwnet.billing.domain.Invoice;
import id.rtrwnet.billing.domain.InvoiceStatus;
import id.rtrwnet.billing.notification.NotificationService;
import id.rtrwnet.billing.repository.CustomerRepository;
import id.rtrwnet.billing.repository.InvoiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
public class BillingService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final NotificationService notification;
    private final BillingProperties properties;
    private final Clock clock;
    private final TransactionTemplate tx;

    public BillingService(InvoiceRepository invoiceRepository, CustomerRepository customerRepository,
                          CustomerService customerService, NotificationService notification,
                          BillingProperties properties, Clock clock, PlatformTransactionManager txManager) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.customerService = customerService;
        this.notification = notification;
        this.properties = properties;
        this.clock = clock;
        this.tx = new TransactionTemplate(txManager);
    }

    /**
     * Terbitkan tagihan periode tertentu untuk semua pelanggan aktif dan terisolir.
     * Idempotent: pelanggan yang sudah punya tagihan di periode itu dilewati.
     *
     * @return jumlah tagihan baru
     */
    @Transactional
    public int generateInvoices(YearMonth period) {
        String periodKey = period.toString();
        List<Customer> customers = customerRepository.findByStatusIn(
                List.of(CustomerStatus.ACTIVE, CustomerStatus.ISOLATED));

        int created = 0;
        for (Customer customer : customers) {
            if (invoiceRepository.existsByCustomerIdAndPeriod(customer.getId(), periodKey)) {
                continue;
            }
            Invoice invoice = new Invoice();
            invoice.setCustomer(customer);
            invoice.setPeriod(periodKey);
            invoice.setAmount(customer.getPlan().getMonthlyPrice());
            invoice.setDueDate(period.atDay(Math.min(customer.getDueDay(), period.lengthOfMonth())));
            invoice.setNumber("INV-%s-%05d".formatted(period.toString().replace("-", ""), customer.getId()));
            invoiceRepository.save(invoice);
            notification.invoiceCreated(invoice);
            created++;
        }
        log.info("Generate tagihan {}: {} tagihan baru dari {} pelanggan", periodKey, created, customers.size());
        return created;
    }

    /**
     * Catat pembayaran, lalu aktifkan kembali pelanggan jika sedang diisolir dan
     * tidak ada lagi tagihan yang lewat batas.
     *
     * Pembayaran di-commit lebih dulu di transaksinya sendiri. Jadi kalau router sedang
     * tidak bisa dihubungi, uang yang diterima tetap tercatat; pelanggan bisa diaktifkan
     * manual lewat endpoint reactivate setelah router normal.
     */
    public Invoice pay(Long invoiceId, String method) {
        Invoice invoice = tx.execute(status -> {
            Invoice i = getInvoice(invoiceId);
            if (i.getStatus() != InvoiceStatus.UNPAID) {
                throw new BusinessException("Tagihan " + i.getNumber() + " berstatus " + i.getStatus());
            }
            i.setStatus(InvoiceStatus.PAID);
            i.setPaidAt(Instant.now(clock));
            i.setPaymentMethod(method.toUpperCase(Locale.ROOT));
            return i;
        });
        log.info("Tagihan {} dibayar via {}", invoice.getNumber(), invoice.getPaymentMethod());

        Customer customer = invoice.getCustomer();
        if (customer.getStatus() == CustomerStatus.ISOLATED && !hasOverdueInvoice(customer.getId())) {
            try {
                customerService.reactivate(customer.getId());
            } catch (RuntimeException e) {
                log.error("Pembayaran {} tercatat, tapi gagal mengaktifkan pelanggan {}: {}",
                        invoice.getNumber(), customer.getCode(), e.getMessage());
            }
        }
        return invoice;
    }

    @Transactional
    public Invoice cancel(Long invoiceId) {
        Invoice invoice = getInvoice(invoiceId);
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessException("Tagihan yang sudah dibayar tidak bisa dibatalkan");
        }
        invoice.setStatus(InvoiceStatus.CANCELLED);
        return invoice;
    }

    /**
     * Isolir pelanggan aktif yang punya tagihan lewat jatuh tempo + masa tenggang.
     * Sengaja tidak dalam satu transaksi: tiap pelanggan diproses di transaksinya sendiri
     * (lewat {@link CustomerService#isolate}), supaya satu router yang error tidak
     * membatalkan isolir pelanggan lain.
     *
     * @return jumlah pelanggan yang berhasil diisolir
     */
    public int isolateOverdueCustomers() {
        LocalDate limit = LocalDate.now(clock).minusDays(properties.graceDays());
        Set<Long> customerIds = new LinkedHashSet<>();
        for (Invoice invoice : invoiceRepository.findByStatusAndDueDateBefore(InvoiceStatus.UNPAID, limit)) {
            if (invoice.getCustomer().getStatus() == CustomerStatus.ACTIVE) {
                customerIds.add(invoice.getCustomer().getId());
            }
        }

        int isolated = 0;
        for (Long id : customerIds) {
            try {
                customerService.isolate(id);
                isolated++;
            } catch (RuntimeException e) {
                log.error("Gagal mengisolir pelanggan id={}: {}", id, e.getMessage());
            }
        }
        log.info("Isolir otomatis: {} dari {} pelanggan menunggak", isolated, customerIds.size());
        return isolated;
    }

    /** Kirim pengingat untuk tagihan yang jatuh tempo dalam beberapa hari ke depan. */
    @Transactional
    public int sendReminders() {
        LocalDate today = LocalDate.now(clock);
        List<Invoice> invoices = invoiceRepository.findByStatusAndReminderSentFalseAndDueDateBetween(
                InvoiceStatus.UNPAID, today, today.plusDays(properties.reminderDaysBefore()));
        for (Invoice invoice : invoices) {
            notification.paymentReminder(invoice);
            invoice.setReminderSent(true);
        }
        return invoices.size();
    }

    @Transactional(readOnly = true)
    public List<Invoice> findByPeriod(YearMonth period) {
        return invoiceRepository.findByPeriodOrderByIdAsc(period.toString());
    }

    @Transactional(readOnly = true)
    public List<Invoice> findByCustomer(Long customerId) {
        return invoiceRepository.findByCustomerIdOrderByPeriodDesc(customerId);
    }

    @Transactional(readOnly = true)
    public Invoice getInvoice(Long id) {
        return invoiceRepository.findWithCustomerById(id).orElseThrow(() -> new NotFoundException("Tagihan", id));
    }

    private boolean hasOverdueInvoice(Long customerId) {
        LocalDate limit = LocalDate.now(clock).minusDays(properties.graceDays());
        return invoiceRepository.existsByCustomerIdAndStatusAndDueDateBefore(customerId, InvoiceStatus.UNPAID, limit);
    }
}
