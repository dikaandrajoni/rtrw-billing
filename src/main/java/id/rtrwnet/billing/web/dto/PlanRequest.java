package id.rtrwnet.billing.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PlanRequest(
        @NotBlank String name,
        @NotBlank String speed,
        @NotNull @DecimalMin("0") BigDecimal monthlyPrice,
        @NotBlank String mikrotikProfile,
        Boolean active
) {
}
