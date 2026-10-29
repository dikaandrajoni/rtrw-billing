package id.rtrwnet.billing.web.dto;

import id.rtrwnet.billing.domain.Plan;

import java.math.BigDecimal;

public record PlanResponse(Long id, String name, String speed, BigDecimal monthlyPrice,
                           String mikrotikProfile, boolean active) {

    public static PlanResponse from(Plan p) {
        return new PlanResponse(p.getId(), p.getName(), p.getSpeed(), p.getMonthlyPrice(),
                p.getMikrotikProfile(), p.isActive());
    }
}
