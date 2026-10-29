package id.rtrwnet.billing.web.dto;

import id.rtrwnet.billing.domain.Ticket;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TicketRequest(
        @NotNull Long customerId,
        @NotBlank @Size(max = 200) String subject,
        @Size(max = 2000) String description,
        Ticket.Category category,
        Ticket.Priority priority
) {
}
