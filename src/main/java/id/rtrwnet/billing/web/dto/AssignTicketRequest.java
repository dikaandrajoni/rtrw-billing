package id.rtrwnet.billing.web.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignTicketRequest(@NotBlank String technician) {
}
