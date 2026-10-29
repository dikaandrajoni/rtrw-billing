package id.rtrwnet.billing.repository;

import id.rtrwnet.billing.domain.Ticket;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @EntityGraph(attributePaths = {"customer"})
    Optional<Ticket> findWithCustomerById(Long id);

    @EntityGraph(attributePaths = {"customer"})
    List<Ticket> findByStatusInOrderByCreatedAtAsc(Collection<Ticket.Status> statuses);

    @EntityGraph(attributePaths = {"customer"})
    List<Ticket> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    long countByStatusIn(Collection<Ticket.Status> statuses);
}
