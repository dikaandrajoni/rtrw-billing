package id.rtrwnet.billing.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Router MikroTik (NAS) yang melayani pelanggan. Diakses lewat REST API RouterOS v7.
 */
@Entity
@Table(name = "routers")
@Getter
@Setter
@NoArgsConstructor
public class Router {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    /** IP atau hostname router. */
    @Column(nullable = false)
    private String host;

    private int port = 80;

    private boolean useHttps = false;

    @Column(nullable = false)
    private String apiUsername;

    @Column(nullable = false)
    private String apiPassword;

    @Enumerated(EnumType.STRING)
    private RouterStatus lastStatus = RouterStatus.UNKNOWN;

    private Instant lastCheckedAt;

    private Integer cpuLoad;

    private String uptime;

    private String routerOsVersion;

    private Integer activePppoeSessions;

    public String baseUrl() {
        return (useHttps ? "https" : "http") + "://" + host + ":" + port + "/rest";
    }
}
