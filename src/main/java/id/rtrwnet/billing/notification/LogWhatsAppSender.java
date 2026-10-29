package id.rtrwnet.billing.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Hanya mencetak pesan ke log. Default untuk development. */
@Slf4j
@Component
@ConditionalOnProperty(name = "billing.whatsapp.provider", havingValue = "LOG", matchIfMissing = true)
public class LogWhatsAppSender implements WhatsAppSender {

    @Override
    public void send(String phone, String message) {
        log.info("[WA -> {}] {}", phone, message.replace('\n', ' '));
    }
}
