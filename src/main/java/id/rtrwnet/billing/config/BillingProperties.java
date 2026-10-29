package id.rtrwnet.billing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "billing")
public record BillingProperties(
        @DefaultValue("RT/RW Net") String companyName,
        /* Hari toleransi setelah jatuh tempo sebelum pelanggan diisolir. */
        @DefaultValue("3") int graceDays,
        /* Kirim pengingat sekian hari sebelum jatuh tempo. */
        @DefaultValue("3") int reminderDaysBefore,
        @DefaultValue Mikrotik mikrotik,
        @DefaultValue Whatsapp whatsapp
) {

    public enum MikrotikMode { FAKE, REST }

    public record Mikrotik(
            /* FAKE untuk development tanpa router, REST untuk RouterOS v7 asli. */
            @DefaultValue("FAKE") MikrotikMode mode,
            /* Nama PPP profile untuk pelanggan yang diisolir. */
            @DefaultValue("isolir") String isolationProfile,
            @DefaultValue("5s") Duration timeout
    ) {
    }

    public enum WhatsappProvider { LOG, FONNTE }

    public record Whatsapp(
            @DefaultValue("LOG") WhatsappProvider provider,
            @DefaultValue("") String token,
            /* Nomor admin/NOC yang menerima alert router down. */
            @DefaultValue("") String adminPhone
    ) {
    }
}
