package id.rtrwnet.billing.web.dto;

import id.rtrwnet.billing.domain.Customer;
import id.rtrwnet.billing.domain.CustomerStatus;

import java.time.Instant;

public record CustomerResponse(Long id, String code, String name, String phone, String address,
                               String pppoeUsername, Long planId, String planName,
                               Long routerId, String routerName, CustomerStatus status,
                               int dueDay, Instant createdAt) {

    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(c.getId(), c.getCode(), c.getName(), c.getPhone(), c.getAddress(),
                c.getPppoeUsername(), c.getPlan().getId(), c.getPlan().getName(),
                c.getRouter().getId(), c.getRouter().getName(), c.getStatus(),
                c.getDueDay(), c.getCreatedAt());
    }
}
