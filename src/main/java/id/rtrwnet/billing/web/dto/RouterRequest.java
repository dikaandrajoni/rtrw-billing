package id.rtrwnet.billing.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RouterRequest(
        @NotBlank String name,
        @NotBlank String host,
        @Min(1) @Max(65535) Integer port,
        Boolean useHttps,
        @NotBlank String apiUsername,
        @NotBlank String apiPassword
) {
}
