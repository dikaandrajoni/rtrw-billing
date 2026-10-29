package id.rtrwnet.billing.service;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String what, Object id) {
        super(what + " dengan id " + id + " tidak ditemukan");
    }
}
