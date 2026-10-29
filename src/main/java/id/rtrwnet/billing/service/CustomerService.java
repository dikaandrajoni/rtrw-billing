package id.rtrwnet.billing.service;

import id.rtrwnet.billing.config.BillingProperties;
import id.rtrwnet.billing.domain.Customer;
import id.rtrwnet.billing.domain.CustomerStatus;
import id.rtrwnet.billing.domain.Plan;
import id.rtrwnet.billing.domain.Router;
import id.rtrwnet.billing.mikrotik.MikrotikGateway;
import id.rtrwnet.billing.notification.NotificationService;
import id.rtrwnet.billing.repository.CustomerRepository;
import id.rtrwnet.billing.repository.PlanRepository;
import id.rtrwnet.billing.repository.RouterRepository;
import id.rtrwnet.billing.web.dto.CustomerRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

/**
 * Siklus hidup pelanggan: daftar, ganti paket, isolir, aktifkan kembali, berhenti.
 * Setiap perubahan status ikut diterapkan ke MikroTik. Jika panggilan ke router gagal,
 * exception membuat transaksi database di-rollback sehingga data tetap sinkron.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PlanRepository planRepository;
    private final RouterRepository routerRepository;
    private final MikrotikGateway mikrotik;
    private final NotificationService notification;
    private final BillingProperties properties;

    @Transactional(readOnly = true)
    public Page<Customer> search(String q, CustomerStatus status, Pageable pageable) {
        String keyword = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        return customerRepository.search(keyword, status, pageable);
    }

    @Transactional(readOnly = true)
    public Customer get(Long id) {
        return customerRepository.findWithDetailsById(id).orElseThrow(() -> new NotFoundException("Pelanggan", id));
    }

    @Transactional
    public Customer register(CustomerRequest request) {
        if (customerRepository.existsByPppoeUsername(request.pppoeUsername())) {
            throw new BusinessException("Username PPPoE '" + request.pppoeUsername() + "' sudah dipakai");
        }
        Plan plan = planRepository.findById(request.planId())
                .orElseThrow(() -> new NotFoundException("Paket", request.planId()));
        if (!plan.isActive()) {
            throw new BusinessException("Paket '" + plan.getName() + "' sudah tidak dijual");
        }
        Router router = routerRepository.findById(request.routerId())
                .orElseThrow(() -> new NotFoundException("Router", request.routerId()));

        Customer customer = new Customer();
        customer.setCode("TMP-" + UUID.randomUUID());
        customer.setName(request.name());
        customer.setPhone(request.phone());
        customer.setAddress(request.address());
        customer.setPppoeUsername(request.pppoeUsername());
        customer.setPppoePassword(request.pppoePassword());
        customer.setPlan(plan);
        customer.setRouter(router);
        customer.setDueDay(request.dueDay() == null ? 10 : request.dueDay());
        customer = customerRepository.saveAndFlush(customer);
        customer.setCode("PLG-%05d".formatted(customer.getId()));

        mikrotik.createPppoeUser(router, customer.getPppoeUsername(), customer.getPppoePassword(), plan.getMikrotikProfile());
        log.info("Pelanggan {} ({}) terdaftar di router {}", customer.getCode(), customer.getName(), router.getName());
        return customer;
    }

    @Transactional
    public Customer changePlan(Long customerId, Long planId) {
        Customer customer = get(customerId);
        requireNotTerminated(customer);
        Plan plan = planRepository.findById(planId).orElseThrow(() -> new NotFoundException("Paket", planId));
        customer.setPlan(plan);
        // Pelanggan yang sedang diisolir tetap di profile isolir; paket baru berlaku saat diaktifkan kembali.
        if (customer.getStatus() == CustomerStatus.ACTIVE) {
            applyProfile(customer, plan.getMikrotikProfile());
        }
        return customer;
    }

    /** Isolir pelanggan. Aman dipanggil ulang: tidak melakukan apa-apa jika sudah diisolir. */
    @Transactional
    public Customer isolate(Long customerId) {
        Customer customer = get(customerId);
        requireNotTerminated(customer);
        if (customer.getStatus() == CustomerStatus.ISOLATED) {
            return customer;
        }
        applyProfile(customer, properties.mikrotik().isolationProfile());
        customer.setStatus(CustomerStatus.ISOLATED);
        notification.customerIsolated(customer);
        log.info("Pelanggan {} diisolir", customer.getCode());
        return customer;
    }

    /** Aktifkan kembali pelanggan. Aman dipanggil ulang. */
    @Transactional
    public Customer reactivate(Long customerId) {
        Customer customer = get(customerId);
        requireNotTerminated(customer);
        if (customer.getStatus() == CustomerStatus.ACTIVE) {
            return customer;
        }
        applyProfile(customer, customer.getPlan().getMikrotikProfile());
        customer.setStatus(CustomerStatus.ACTIVE);
        notification.customerReactivated(customer);
        log.info("Pelanggan {} diaktifkan kembali", customer.getCode());
        return customer;
    }

    @Transactional
    public Customer terminate(Long customerId) {
        Customer customer = get(customerId);
        if (customer.getStatus() == CustomerStatus.TERMINATED) {
            return customer;
        }
        mikrotik.disconnectActiveSession(customer.getRouter(), customer.getPppoeUsername());
        mikrotik.removePppoeUser(customer.getRouter(), customer.getPppoeUsername());
        customer.setStatus(CustomerStatus.TERMINATED);
        return customer;
    }

    private void applyProfile(Customer customer, String profile) {
        mikrotik.changePppoeProfile(customer.getRouter(), customer.getPppoeUsername(), profile);
        // Putus sesi supaya profile baru langsung berlaku saat modem pelanggan reconnect.
        mikrotik.disconnectActiveSession(customer.getRouter(), customer.getPppoeUsername());
    }

    private static void requireNotTerminated(Customer customer) {
        if (customer.getStatus() == CustomerStatus.TERMINATED) {
            throw new BusinessException("Pelanggan " + customer.getCode() + " sudah berhenti berlangganan");
        }
    }
}
