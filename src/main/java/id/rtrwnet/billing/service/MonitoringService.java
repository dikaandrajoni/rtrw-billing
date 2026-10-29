package id.rtrwnet.billing.service;

import id.rtrwnet.billing.domain.Router;
import id.rtrwnet.billing.domain.RouterStatus;
import id.rtrwnet.billing.mikrotik.MikrotikGateway;
import id.rtrwnet.billing.mikrotik.MikrotikGateway.RouterHealth;
import id.rtrwnet.billing.notification.NotificationService;
import id.rtrwnet.billing.repository.RouterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Cek kesehatan router secara berkala (NOC sederhana): status UP/DOWN, CPU, uptime,
 * dan jumlah sesi PPPoE aktif. Alert WA dikirim hanya saat status berubah,
 * supaya admin tidak dibanjiri pesan yang sama tiap menit.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final RouterRepository routerRepository;
    private final MikrotikGateway mikrotik;
    private final NotificationService notification;
    private final Clock clock;

    /** Cek semua router. Satu router lambat/error tidak menghentikan pengecekan router lain. */
    public void checkAllRouters() {
        for (Router router : routerRepository.findAll()) {
            try {
                check(router.getId());
            } catch (RuntimeException e) {
                log.error("Monitoring router id={} gagal: {}", router.getId(), e.getMessage());
            }
        }
    }

    @Transactional
    public Router check(Long routerId) {
        Router router = routerRepository.findById(routerId).orElseThrow(() -> new NotFoundException("Router", routerId));
        RouterStatus previous = router.getLastStatus();

        try {
            RouterHealth health = mikrotik.getHealth(router);
            router.setLastStatus(RouterStatus.UP);
            router.setCpuLoad(health.cpuLoad());
            router.setUptime(health.uptime());
            router.setRouterOsVersion(health.version());
            router.setActivePppoeSessions(health.activePppoeSessions());
        } catch (RuntimeException e) {
            router.setLastStatus(RouterStatus.DOWN);
            log.warn("Router {} tidak bisa dihubungi: {}", router.getName(), e.getMessage());
        }
        router.setLastCheckedAt(Instant.now(clock));

        if (previous != RouterStatus.DOWN && router.getLastStatus() == RouterStatus.DOWN) {
            notification.routerDown(router);
        } else if (previous == RouterStatus.DOWN && router.getLastStatus() == RouterStatus.UP) {
            notification.routerRecovered(router);
        }
        // save eksplisit: checkAllRouters memanggil method ini dari dalam class yang sama,
        // jadi @Transactional tidak aktif di jalur itu.
        return routerRepository.save(router);
    }
}
