package id.rtrwnet.billing.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TicketNoteRequest(@NotBlank @Size(max = 500) String note) {
}
