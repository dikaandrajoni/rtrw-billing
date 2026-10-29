package id.rtrwnet.billing.domain;

public enum CustomerStatus {
    /** Internet jalan normal. */
    ACTIVE,
    /** Diisolir karena menunggak; PPP profile diganti ke profile isolir. */
    ISOLATED,
    /** Berhenti berlangganan; PPP secret dihapus dari router. */
    TERMINATED
}
