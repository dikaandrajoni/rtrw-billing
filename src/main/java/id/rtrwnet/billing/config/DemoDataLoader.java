package id.rtrwnet.billing.config;

import id.rtrwnet.billing.repository.PlanRepository;
import id.rtrwnet.billing.service.CustomerService;
import id.rtrwnet.billing.service.PlanService;
import id.rtrwnet.billing.service.RouterService;
import id.rtrwnet.billing.web.dto.CustomerRequest;
import id.rtrwnet.billing.web.dto.PlanRequest;
import id.rtrwnet.billing.web.dto.RouterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Data contoh untuk demo. Hanya jalan jika billing.demo-data=true dan database kosong. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "billing.demo-data", havingValue = "true")
public class DemoDataLoader implements ApplicationRunner {

    private final PlanRepository planRepository;
    private final PlanService planService;
    private final RouterService routerService;
    private final CustomerService customerService;

    @Override
    public void run(ApplicationArguments args) {
        if (planRepository.count() > 0) {
            return;
        }
        var p10 = planService.create(new PlanRequest("Home 10 Mbps", "10M/10M", new BigDecimal("150000"), "paket-10m", true));
        var p20 = planService.create(new PlanRequest("Home 20 Mbps", "20M/20M", new BigDecimal("200000"), "paket-20m", true));
        planService.create(new PlanRequest("Home 50 Mbps", "50M/50M", new BigDecimal("300000"), "paket-50m", true));

        var router = routerService.create(new RouterRequest("NAS-Pusat", "192.168.88.1", 80, false, "billing", "rahasia"));

        customerService.register(new CustomerRequest("Budi Santoso", "6281200000001", "Jl. Merdeka 1",
                "budi", "budi12345", p10.getId(), router.getId(), 10));
        customerService.register(new CustomerRequest("Siti Aminah", "6281200000002", "Jl. Merdeka 2",
                "siti", "siti12345", p20.getId(), router.getId(), 5));
        customerService.register(new CustomerRequest("Andi Wijaya", "6281200000003", "Jl. Merdeka 3",
                "andi", "andi12345", p10.getId(), router.getId(), 20));
        log.info("Data demo dibuat: 3 paket, 1 router, 3 pelanggan");
    }
}
