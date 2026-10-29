package id.rtrwnet.billing.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nomor pelanggan, misalnya "PLG-00012". */
    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    /** Nomor WhatsApp, format 62xxxxxxxxxx. */
    @Column(nullable = false)
    private String phone;

    private String address;

    @Column(nullable = false, unique = true)
    private String pppoeUsername;

    @Column(nullable = false)
    private String pppoePassword;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Plan plan;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Router router;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerStatus status = CustomerStatus.ACTIVE;

    /** Tanggal jatuh tempo setiap bulan (1-28). */
    private int dueDay = 10;

    private Instant createdAt = Instant.now();
}
