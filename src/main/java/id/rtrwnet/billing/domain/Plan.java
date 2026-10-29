package id.rtrwnet.billing.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Paket internet, misalnya "Home 10 Mbps".
 * {@code mikrotikProfile} adalah nama PPP profile di MikroTik yang mengatur rate-limit.
 */
@Entity
@Table(name = "plans")
@Getter
@Setter
@NoArgsConstructor
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    /** Contoh: "10M/10M" (upload/download), hanya untuk ditampilkan. */
    @Column(nullable = false)
    private String speed;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyPrice;

    /** Nama PPP profile di MikroTik, misalnya "paket-10m". */
    @Column(nullable = false)
    private String mikrotikProfile;

    private boolean active = true;
}
