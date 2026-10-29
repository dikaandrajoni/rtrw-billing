package id.rtrwnet.billing.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Contoh method: CASH, TRANSFER, QRIS. */
public record PaymentRequest(@NotBlank String method) {
}
