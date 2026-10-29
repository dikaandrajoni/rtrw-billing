package id.rtrwnet.billing.web.dto;

import id.rtrwnet.billing.domain.Router;
import id.rtrwnet.billing.domain.RouterStatus;

import java.time.Instant;

/** Password API sengaja tidak ikut dikembalikan. */
public record RouterResponse(Long id, String name, String host, int port, boolean useHttps,
                             RouterStatus lastStatus, Instant lastCheckedAt, Integer cpuLoad,
                             String uptime, String routerOsVersion, Integer activePppoeSessions) {

    public static RouterResponse from(Router r) {
        return new RouterResponse(r.getId(), r.getName(), r.getHost(), r.getPort(), r.isUseHttps(),
                r.getLastStatus(), r.getLastCheckedAt(), r.getCpuLoad(), r.getUptime(),
                r.getRouterOsVersion(), r.getActivePppoeSessions());
    }
}
