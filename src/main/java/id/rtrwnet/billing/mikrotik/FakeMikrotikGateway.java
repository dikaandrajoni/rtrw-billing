package id.rtrwnet.billing.mikrotik;

import id.rtrwnet.billing.domain.Router;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Router tiruan di memori. Aktif saat {@code billing.mikrotik.mode=FAKE} (default),
 * sehingga aplikasi bisa dicoba tanpa punya MikroTik.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "billing.mikrotik.mode", havingValue = "FAKE", matchIfMissing = true)
public class FakeMikrotikGateway implements MikrotikGateway {

    /** key: routerId:username, value: profile */
    private final Map<String, String> secrets = new ConcurrentHashMap<>();

    @Override
    public void createPppoeUser(Router router, String username, String password, String profile) {
        String key = key(router, username);
        if (secrets.putIfAbsent(key, profile) != null) {
            throw new MikrotikException("PPP secret '" + username + "' sudah ada di router " + router.getName());
        }
        log.info("[FAKE MikroTik {}] buat PPPoE {} profile={}", router.getName(), username, profile);
    }

    @Override
    public void changePppoeProfile(Router router, String username, String profile) {
        // Data fake hilang saat aplikasi restart, sedangkan database tetap ada.
        // Jadi user yang belum dikenal dibuat ulang saja, bukan dianggap error.
        secrets.put(key(router, username), profile);
        log.info("[FAKE MikroTik {}] ganti profile {} -> {}", router.getName(), username, profile);
    }

    @Override
    public void removePppoeUser(Router router, String username) {
        secrets.remove(key(router, username));
        log.info("[FAKE MikroTik {}] hapus PPPoE {}", router.getName(), username);
    }

    @Override
    public void disconnectActiveSession(Router router, String username) {
        log.info("[FAKE MikroTik {}] putus sesi aktif {}", router.getName(), username);
    }

    @Override
    public RouterHealth getHealth(Router router) {
        long sessions = secrets.keySet().stream().filter(k -> k.startsWith(router.getId() + ":")).count();
        return new RouterHealth("1d2h3m", 7, "7.16 (fake)", (int) sessions);
    }

    /** Untuk test: profile yang sedang terpasang pada user. */
    public String profileOf(Router router, String username) {
        return secrets.get(key(router, username));
    }

    private String key(Router router, String username) {
        return router.getId() + ":" + username;
    }
}
