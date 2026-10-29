package id.rtrwnet.billing.notification;

import id.rtrwnet.billing.config.BillingProperties;
import id.rtrwnet.billing.domain.Customer;
import id.rtrwnet.billing.domain.Invoice;
import id.rtrwnet.billing.domain.Router;
import id.rtrwnet.billing.domain.Ticket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Menyusun isi pesan dan mengirimnya. Kegagalan kirim hanya dicatat di log,
 * supaya proses billing tidak ikut gagal karena gateway WA bermasalah.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Locale ID = Locale.forLanguageTag("id-ID");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", ID);

    private final WhatsAppSender sender;
    private final BillingProperties properties;

    public void invoiceCreated(Invoice invoice) {
        Customer c = invoice.getCustomer();
        send(c.getPhone(), """
                Halo %s,
                Tagihan internet %s periode %s sebesar %s sudah terbit.
                No. tagihan: %s
                Jatuh tempo: %s
                Terima kasih.""".formatted(
                c.getName(), properties.companyName(), invoice.getPeriod(),
                rupiah(invoice.getAmount()), invoice.getNumber(), invoice.getDueDate().format(DATE)));
    }

    public void paymentReminder(Invoice invoice) {
        Customer c = invoice.getCustomer();
        send(c.getPhone(), """
                Halo %s, pengingat: tagihan %s sebesar %s jatuh tempo %s.
                Mohon dibayar agar layanan tidak terisolir.""".formatted(
                c.getName(), invoice.getNumber(), rupiah(invoice.getAmount()), invoice.getDueDate().format(DATE)));
    }

    public void customerIsolated(Customer c) {
        send(c.getPhone(), """
                Halo %s, layanan internet Anda sementara diisolir karena ada tagihan yang belum dibayar.
                Layanan aktif kembali otomatis setelah pembayaran diterima.""".formatted(c.getName()));
    }

    public void customerReactivated(Customer c) {
        send(c.getPhone(), "Halo %s, pembayaran diterima dan layanan internet Anda sudah aktif kembali. Terima kasih."
                .formatted(c.getName()));
    }

    public void ticketUpdated(Ticket t) {
        send(t.getCustomer().getPhone(), "Tiket %s (%s) sekarang berstatus %s."
                .formatted(t.getNumber(), t.getSubject(), t.getStatus()));
    }

    public void routerDown(Router r) {
        sendToAdmin("⚠️ Router %s (%s) DOWN / tidak bisa dihubungi.".formatted(r.getName(), r.getHost()));
    }

    public void routerRecovered(Router r) {
        sendToAdmin("✅ Router %s (%s) kembali UP.".formatted(r.getName(), r.getHost()));
    }

    private void sendToAdmin(String message) {
        String admin = properties.whatsapp().adminPhone();
        if (admin == null || admin.isBlank()) {
            log.warn("Admin phone belum diset, alert tidak dikirim: {}", message);
            return;
        }
        send(admin, message);
    }

    private void send(String phone, String message) {
        try {
            sender.send(phone, message);
        } catch (RuntimeException e) {
            log.error("Gagal kirim WA ke {}: {}", phone, e.getMessage());
        }
    }

    static String rupiah(BigDecimal amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(ID);
        format.setMaximumFractionDigits(0);
        return format.format(amount);
    }
}
