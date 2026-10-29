package id.rtrwnet.billing.web;

import id.rtrwnet.billing.service.MonitoringService;
import id.rtrwnet.billing.service.RouterService;
import id.rtrwnet.billing.web.dto.RouterRequest;
import id.rtrwnet.billing.web.dto.RouterResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routers")
@RequiredArgsConstructor
public class RouterController {

    private final RouterService routerService;
    private final MonitoringService monitoringService;

    @GetMapping
    public List<RouterResponse> list() {
        return routerService.findAll().stream().map(RouterResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RouterResponse create(@Valid @RequestBody RouterRequest request) {
        return RouterResponse.from(routerService.create(request));
    }

    @PutMapping("/{id}")
    public RouterResponse update(@PathVariable Long id, @Valid @RequestBody RouterRequest request) {
        return RouterResponse.from(routerService.update(id, request));
    }

    /** Cek kesehatan router sekarang juga, tanpa menunggu jadwal monitoring. */
    @PostMapping("/{id}/check")
    public RouterResponse check(@PathVariable Long id) {
        return RouterResponse.from(monitoringService.check(id));
    }
}
