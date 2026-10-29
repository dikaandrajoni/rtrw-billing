package id.rtrwnet.billing.repository;

import id.rtrwnet.billing.domain.Router;
import id.rtrwnet.billing.domain.RouterStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouterRepository extends JpaRepository<Router, Long> {
    long countByLastStatus(RouterStatus status);
}
