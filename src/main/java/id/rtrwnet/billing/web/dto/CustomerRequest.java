package id.rtrwnet.billing.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "^62\\d{8,13}$", message = "format nomor harus 62xxxxxxxxxx") String phone,
        String address,
        @NotBlank @Pattern(regexp = "^[a-zA-Z0-9._-]{3,32}$", message = "hanya huruf, angka, titik, _ atau -") String pppoeUsername,
        @NotBlank @Size(min = 6, max = 64) String pppoePassword,
        @NotNull Long planId,
        @NotNull Long routerId,
        @Min(1) @Max(28) Integer dueDay
) {
}
