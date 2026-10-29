package id.rtrwnet.billing.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Tiket gangguan atau keluhan pelanggan. */
@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
public class Ticket {

    public enum Category { NO_INTERNET, SLOW, BILLING, INSTALLATION, OTHER }

    public enum Priority { LOW, MEDIUM, HIGH }

    public enum Status { OPEN, IN_PROGRESS, RESOLVED, CLOSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String number;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Customer customer;

    @Column(nullable = false)
    private String subject;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category = Category.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OPEN;

    /** Nama teknisi yang menangani. */
    private String assignee;

    /** Catatan pengerjaan, ditambahkan berurutan. */
    @Column(length = 4000)
    private String notes;

    private Instant createdAt = Instant.now();

    private Instant resolvedAt;
}
