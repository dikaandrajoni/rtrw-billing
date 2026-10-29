package id.rtrwnet.billing.service;

/** Pelanggaran aturan bisnis, misalnya membayar tagihan yang sudah lunas. */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
