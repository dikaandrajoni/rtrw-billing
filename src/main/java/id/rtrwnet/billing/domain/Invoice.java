package id.rtrwnet.billing.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Tagihan bulanan. Satu pelanggan hanya punya satu tagihan per periode
 * (dijaga oleh unique constraint), sehingga generate tagihan aman dijalankan ulang.
 */
@Entity
@Table(name = "invoices",
        uniqueConstraints = @UniqueConstraint(columnNames = {"customer_id", "period"}))
@Getter
@Setter
@NoArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String number;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Customer customer;

    /** Periode tagihan, format yyyy-MM. */
    @Column(nullable = false, length = 7)
    private String period;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.UNPAID;

    private Instant paidAt;

    /** Contoh: CASH, TRANSFER, QRIS. */
    private String paymentMethod;

    private boolean reminderSent = false;

    @Version
    private Long version;

    private Instant createdAt = Instant.now();
}
