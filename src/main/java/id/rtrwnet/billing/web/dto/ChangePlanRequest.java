package id.rtrwnet.billing.web.dto;

import jakarta.validation.constraints.NotNull;

public record ChangePlanRequest(@NotNull Long planId) {
}
