package id.rtrwnet.billing.mikrotik;

import id.rtrwnet.billing.config.BillingProperties;
import id.rtrwnet.billing.domain.Router;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementasi memakai REST API RouterOS v7 (https://help.mikrotik.com/docs/display/ROS/REST+API).
 *
 * Syarat di router:
 *   /ip service enable www          (atau www-ssl untuk HTTPS)
 *   /user group add name=billing policy=read,write,api,rest-api,test
 *   /user add name=billing group=billing password=...
 *   /ppp profile add name=isolir rate-limit=256k/256k   (plus aturan redirect ke halaman tagihan)
 *
 * Aktif saat {@code billing.mikrotik.mode=REST}.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "billing.mikrotik.mode", havingValue = "REST")
public class RestMikrotikGateway implements MikrotikGateway {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_OF_MAPS =
            new ParameterizedTypeReference<>() {
            };

    private final BillingProperties properties;

    public RestMikrotikGateway(BillingProperties properties) {
        this.properties = properties;
    }

    @Override
    public void createPppoeUser(Router router, String username, String password, String profile) {
        call(router, "membuat PPPoE " + username, client -> client.put()
                .uri("/ppp/secret")
                .body(Map.of("name", username, "password", password, "service", "pppoe", "profile", profile))
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public void changePppoeProfile(Router router, String username, String profile) {
        String id = findId(router, "/ppp/secret", username)
                .orElseThrow(() -> new MikrotikException("PPP secret '" + username + "' tidak ditemukan di " + router.getName()));
        call(router, "mengganti profile " + username, client -> client.patch()
                .uri("/ppp/secret/{id}", id)
                .body(Map.of("profile", profile))
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public void removePppoeUser(Router router, String username) {
        findId(router, "/ppp/secret", username).ifPresent(id ->
                call(router, "menghapus PPPoE " + username, client -> client.delete()
                        .uri("/ppp/secret/{id}", id)
                        .retrieve()
                        .toBodilessEntity()));
    }

    @Override
    public void disconnectActiveSession(Router router, String username) {
        findId(router, "/ppp/active", username).ifPresent(id ->
                call(router, "memutus sesi " + username, client -> client.delete()
                        .uri("/ppp/active/{id}", id)
                        .retrieve()
                        .toBodilessEntity()));
    }

    @Override
    public RouterHealth getHealth(Router router) {
        Map<String, Object> resource = call(router, "membaca system resource", client -> client.get()
                .uri("/system/resource")
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                }));
        List<Map<String, Object>> active = call(router, "membaca PPP active", client -> client.get()
                .uri("/ppp/active?.proplist=name")
                .retrieve()
                .body(LIST_OF_MAPS));

        return new RouterHealth(
                String.valueOf(resource.getOrDefault("uptime", "-")),
                parseInt(resource.get("cpu-load")),
                String.valueOf(resource.getOrDefault("version", "-")),
                active == null ? 0 : active.size());
    }

    private Optional<String> findId(Router router, String path, String username) {
        List<Map<String, Object>> rows = call(router, "mencari " + username + " di " + path, client -> client.get()
                .uri(path + "?name={name}", username)
                .retrieve()
                .body(LIST_OF_MAPS));
        if (rows == null || rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(rows.get(0).get(".id")).map(Object::toString);
    }

    private <T> T call(Router router, String action, java.util.function.Function<RestClient, T> fn) {
        try {
            return fn.apply(client(router));
        } catch (RestClientException e) {
            log.warn("Gagal {} di router {}: {}", action, router.getName(), e.getMessage());
            throw new MikrotikException("Gagal " + action + " di router " + router.getName() + ": " + e.getMessage(), e);
        }
    }

    private RestClient client(Router router) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.mikrotik().timeout());
        factory.setReadTimeout(properties.mikrotik().timeout());
        return RestClient.builder()
                .baseUrl(router.baseUrl())
                .requestFactory(factory)
                .defaultHeaders(h -> h.setBasicAuth(router.getApiUsername(), router.getApiPassword()))
                .build();
    }

    private static int parseInt(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
