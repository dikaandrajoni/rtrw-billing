package id.rtrwnet.billing.service;

import id.rtrwnet.billing.domain.Router;
import id.rtrwnet.billing.repository.RouterRepository;
import id.rtrwnet.billing.web.dto.RouterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RouterService {

    private final RouterRepository routerRepository;

    @Transactional(readOnly = true)
    public List<Router> findAll() {
        return routerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Router get(Long id) {
        return routerRepository.findById(id).orElseThrow(() -> new NotFoundException("Router", id));
    }

    @Transactional
    public Router create(RouterRequest request) {
        Router router = new Router();
        apply(router, request);
        return routerRepository.save(router);
    }

    @Transactional
    public Router update(Long id, RouterRequest request) {
        Router router = get(id);
        apply(router, request);
        return router;
    }

    private void apply(Router router, RouterRequest request) {
        router.setName(request.name());
        router.setHost(request.host());
        boolean https = Boolean.TRUE.equals(request.useHttps());
        router.setUseHttps(https);
        router.setPort(request.port() != null ? request.port() : (https ? 443 : 80));
        router.setApiUsername(request.apiUsername());
        router.setApiPassword(request.apiPassword());
    }
}
