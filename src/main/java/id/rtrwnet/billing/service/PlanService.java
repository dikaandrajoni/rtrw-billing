package id.rtrwnet.billing.service;

import id.rtrwnet.billing.domain.Plan;
import id.rtrwnet.billing.repository.PlanRepository;
import id.rtrwnet.billing.web.dto.PlanRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;

    @Transactional(readOnly = true)
    public List<Plan> findAll() {
        return planRepository.findAll();
    }

    @Transactional
    public Plan create(PlanRequest request) {
        if (planRepository.existsByName(request.name())) {
            throw new BusinessException("Paket dengan nama '" + request.name() + "' sudah ada");
        }
        Plan plan = new Plan();
        apply(plan, request);
        return planRepository.save(plan);
    }

    /** Perubahan harga berlaku untuk tagihan periode berikutnya; tagihan yang sudah terbit tidak berubah. */
    @Transactional
    public Plan update(Long id, PlanRequest request) {
        Plan plan = planRepository.findById(id).orElseThrow(() -> new NotFoundException("Paket", id));
        apply(plan, request);
        return plan;
    }

    private void apply(Plan plan, PlanRequest request) {
        plan.setName(request.name());
        plan.setSpeed(request.speed());
        plan.setMonthlyPrice(request.monthlyPrice());
        plan.setMikrotikProfile(request.mikrotikProfile());
        plan.setActive(request.active() == null || request.active());
    }
}
