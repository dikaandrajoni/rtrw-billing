package id.rtrwnet.billing.mikrotik;

public class MikrotikException extends RuntimeException {

    public MikrotikException(String message) {
        super(message);
    }

    public MikrotikException(String message, Throwable cause) {
        super(message, cause);
    }
}
