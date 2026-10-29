package id.rtrwnet.billing.scheduler;

import id.rtrwnet.billing.service.BillingService;
import id.rtrwnet.billing.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.YearMonth;

/**
 * Job otomatis. Jadwal bisa diubah di application.yml (billing.schedule.*).
 * Bisa dimatikan dengan billing.schedule.enabled=false (misalnya di test).
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "billing.schedule.enabled", havingValue = "true", matchIfMissing = true)
public class BillingJobs {

    private final BillingService billingService;
    private final MonitoringService monitoringService;
    private final Clock clock;

    /** Tanggal 1 jam 06:00: terbitkan tagihan bulan berjalan. */
    @Scheduled(cron = "${billing.schedule.generate-invoices:0 0 6 1 * *}", zone = "Asia/Jakarta")
    public void generateMonthlyInvoices() {
        billingService.generateInvoices(YearMonth.now(clock));
    }

    /** Setiap hari jam 09:00: kirim pengingat tagihan yang mendekati jatuh tempo. */
    @Scheduled(cron = "${billing.schedule.reminders:0 0 9 * * *}", zone = "Asia/Jakarta")
    public void sendReminders() {
        billingService.sendReminders();
    }

    /** Setiap hari jam 00:05: isolir pelanggan yang menunggak melewati masa tenggang. */
    @Scheduled(cron = "${billing.schedule.isolate-overdue:0 5 0 * * *}", zone = "Asia/Jakarta")
    public void isolateOverdue() {
        billingService.isolateOverdueCustomers();
    }

    /** Setiap 1 menit: cek kesehatan semua router. */
    @Scheduled(fixedDelayString = "${billing.schedule.monitor-interval-ms:60000}", initialDelayString = "10000")
    public void monitorRouters() {
        monitoringService.checkAllRouters();
    }
}
