package id.rtrwnet.billing.service;

import id.rtrwnet.billing.domain.Customer;
import id.rtrwnet.billing.domain.Ticket;
import id.rtrwnet.billing.notification.NotificationService;
import id.rtrwnet.billing.repository.CustomerRepository;
import id.rtrwnet.billing.repository.TicketRepository;
import id.rtrwnet.billing.web.dto.TicketRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TicketService {

    public static final Set<Ticket.Status> OPEN_STATUSES = EnumSet.of(Ticket.Status.OPEN, Ticket.Status.IN_PROGRESS);

    private static final DateTimeFormatter NUMBER_DATE = DateTimeFormatter.ofPattern("yyMMdd");

    private final TicketRepository ticketRepository;
    private final CustomerRepository customerRepository;
    private final NotificationService notification;
    private final Clock clock;

    @Transactional
    public Ticket open(TicketRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new NotFoundException("Pelanggan", request.customerId()));
        Ticket ticket = new Ticket();
        ticket.setNumber("TMP-" + java.util.UUID.randomUUID());
        ticket.setCustomer(customer);
        ticket.setSubject(request.subject());
        ticket.setDescription(request.description());
        if (request.category() != null) {
            ticket.setCategory(request.category());
        }
        if (request.priority() != null) {
            ticket.setPriority(request.priority());
        } else if (request.category() == Ticket.Category.NO_INTERNET) {
            ticket.setPriority(Ticket.Priority.HIGH);
        }
        ticket.setCreatedAt(Instant.now(clock));
        ticket = ticketRepository.saveAndFlush(ticket);
        ticket.setNumber("TKT-%s-%04d".formatted(LocalDate.now(clock).format(NUMBER_DATE), ticket.getId()));
        notification.ticketUpdated(ticket);
        return ticket;
    }

    @Transactional
    public Ticket assign(Long id, String technician) {
        Ticket ticket = getOpen(id);
        ticket.setAssignee(technician);
        ticket.setStatus(Ticket.Status.IN_PROGRESS);
        appendNote(ticket, "Ditugaskan ke " + technician);
        notification.ticketUpdated(ticket);
        return ticket;
    }

    @Transactional
    public Ticket addNote(Long id, String note) {
        Ticket ticket = get(id);
        appendNote(ticket, note);
        return ticket;
    }

    @Transactional
    public Ticket resolve(Long id, String note) {
        Ticket ticket = getOpen(id);
        ticket.setStatus(Ticket.Status.RESOLVED);
        ticket.setResolvedAt(Instant.now(clock));
        appendNote(ticket, "Selesai: " + note);
        notification.ticketUpdated(ticket);
        return ticket;
    }

    @Transactional
    public Ticket close(Long id) {
        Ticket ticket = get(id);
        if (ticket.getStatus() != Ticket.Status.RESOLVED) {
            throw new BusinessException("Hanya tiket berstatus RESOLVED yang bisa ditutup");
        }
        ticket.setStatus(Ticket.Status.CLOSED);
        return ticket;
    }

    @Transactional(readOnly = true)
    public List<Ticket> findOpen() {
        // Prioritas disimpan sebagai teks, jadi urutkan di Java: HIGH dulu, lalu yang paling lama menunggu.
        return ticketRepository.findByStatusInOrderByCreatedAtAsc(OPEN_STATUSES).stream()
                .sorted(java.util.Comparator.comparing((Ticket t) -> t.getPriority().ordinal()).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Ticket> findByCustomer(Long customerId) {
        return ticketRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Transactional(readOnly = true)
    public Ticket get(Long id) {
        return ticketRepository.findWithCustomerById(id).orElseThrow(() -> new NotFoundException("Tiket", id));
    }

    private Ticket getOpen(Long id) {
        Ticket ticket = get(id);
        if (!OPEN_STATUSES.contains(ticket.getStatus())) {
            throw new BusinessException("Tiket " + ticket.getNumber() + " sudah " + ticket.getStatus());
        }
        return ticket;
    }

    private void appendNote(Ticket ticket, String note) {
        String line = "[" + Instant.now(clock) + "] " + note;
        String notes = ticket.getNotes() == null ? line : ticket.getNotes() + "\n" + line;
        if (notes.length() > 4000) {
            notes = notes.substring(notes.length() - 4000);
        }
        ticket.setNotes(notes);
    }
}
