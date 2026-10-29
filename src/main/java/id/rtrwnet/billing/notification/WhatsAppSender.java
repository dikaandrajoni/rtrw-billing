package id.rtrwnet.billing.notification;

public interface WhatsAppSender {

    /**
     * @param phone   nomor tujuan format 62xxxxxxxxxx
     * @param message isi pesan
     */
    void send(String phone, String message);
}
