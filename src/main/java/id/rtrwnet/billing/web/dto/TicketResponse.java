package id.rtrwnet.billing.web.dto;

import id.rtrwnet.billing.domain.Ticket;

import java.time.Instant;

public record TicketResponse(Long id, String number, Long customerId, String customerName,
                             String subject, String description, Ticket.Category category,
                             Ticket.Priority priority, Ticket.Status status, String assignee,
                             String notes, Instant createdAt, Instant resolvedAt) {

    public static TicketResponse from(Ticket t) {
        return new TicketResponse(t.getId(), t.getNumber(), t.getCustomer().getId(), t.getCustomer().getName(),
                t.getSubject(), t.getDescription(), t.getCategory(), t.getPriority(), t.getStatus(),
                t.getAssignee(), t.getNotes(), t.getCreatedAt(), t.getResolvedAt());
    }
}
