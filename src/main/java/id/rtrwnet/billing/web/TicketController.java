package id.rtrwnet.billing.web;

import id.rtrwnet.billing.service.TicketService;
import id.rtrwnet.billing.web.dto.AssignTicketRequest;
import id.rtrwnet.billing.web.dto.TicketNoteRequest;
import id.rtrwnet.billing.web.dto.TicketRequest;
import id.rtrwnet.billing.web.dto.TicketResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    /** Antrian tiket yang belum selesai, prioritas tinggi di atas. */
    @GetMapping
    public List<TicketResponse> openTickets() {
        return ticketService.findOpen().stream().map(TicketResponse::from).toList();
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable Long id) {
        return TicketResponse.from(ticketService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse open(@Valid @RequestBody TicketRequest request) {
        return TicketResponse.from(ticketService.open(request));
    }

    @PostMapping("/{id}/assign")
    public TicketResponse assign(@PathVariable Long id, @Valid @RequestBody AssignTicketRequest request) {
        return TicketResponse.from(ticketService.assign(id, request.technician()));
    }

    @PostMapping("/{id}/notes")
    public TicketResponse addNote(@PathVariable Long id, @Valid @RequestBody TicketNoteRequest request) {
        return TicketResponse.from(ticketService.addNote(id, request.note()));
    }

    @PostMapping("/{id}/resolve")
    public TicketResponse resolve(@PathVariable Long id, @Valid @RequestBody TicketNoteRequest request) {
        return TicketResponse.from(ticketService.resolve(id, request.note()));
    }

    @PostMapping("/{id}/close")
    public TicketResponse close(@PathVariable Long id) {
        return TicketResponse.from(ticketService.close(id));
    }
}
