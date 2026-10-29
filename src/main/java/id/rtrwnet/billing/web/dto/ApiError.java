package id.rtrwnet.billing.web.dto;

import java.time.Instant;
import java.util.Map;

public record ApiError(Instant timestamp, int status, String error, String message, Map<String, String> fields) {
}
