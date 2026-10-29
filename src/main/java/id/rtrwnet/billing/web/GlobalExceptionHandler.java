package id.rtrwnet.billing.web;

import id.rtrwnet.billing.mikrotik.MikrotikException;
import id.rtrwnet.billing.service.BusinessException;
import id.rtrwnet.billing.service.NotFoundException;
import id.rtrwnet.billing.web.dto.ApiError;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> notFound(NotFoundException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage(), null);
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiError> business(BusinessException e) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> conflict(DataIntegrityViolationException e) {
        return error(HttpStatus.CONFLICT, "Data bentrok dengan data yang sudah ada", null);
    }

    /** Router tidak bisa dihubungi atau menolak perintah. */
    @ExceptionHandler(MikrotikException.class)
    ResponseEntity<ApiError> mikrotik(MikrotikException e) {
        return error(HttpStatus.BAD_GATEWAY, e.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "Input tidak valid", fields);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message, Map<String, String> fields) {
        return ResponseEntity.status(status)
                .body(new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, fields));
    }
}
