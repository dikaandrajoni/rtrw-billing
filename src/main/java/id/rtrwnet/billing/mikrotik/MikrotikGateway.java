package id.rtrwnet.billing.mikrotik;

import id.rtrwnet.billing.domain.Router;

/**
 * Operasi ke router MikroTik yang dibutuhkan aplikasi billing.
 * Ada dua implementasi: {@link RestMikrotikGateway} (RouterOS v7 asli)
 * dan {@link FakeMikrotikGateway} (untuk development dan test).
 */
public interface MikrotikGateway {

    /** Membuat PPP secret (user PPPoE) baru. */
    void createPppoeUser(Router router, String username, String password, String profile);

    /** Mengganti PPP profile user; dipakai untuk ganti paket dan isolir. */
    void changePppoeProfile(Router router, String username, String profile);

    /** Menghapus PPP secret. Tidak error jika user sudah tidak ada. */
    void removePppoeUser(Router router, String username);

    /**
     * Memutus sesi aktif supaya profile baru langsung berlaku saat pelanggan reconnect.
     * Tidak error jika user sedang offline.
     */
    void disconnectActiveSession(Router router, String username);

    /** Mengambil kondisi router. Melempar {@link MikrotikException} jika router tidak bisa dihubungi. */
    RouterHealth getHealth(Router router);

    record RouterHealth(String uptime, int cpuLoad, String version, int activePppoeSessions) {
    }
}
