package id.rtrwnet.billing.repository;

import id.rtrwnet.billing.domain.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    boolean existsByName(String name);
}
