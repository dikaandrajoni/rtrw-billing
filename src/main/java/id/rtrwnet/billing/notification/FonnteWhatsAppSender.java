package id.rtrwnet.billing.notification;

import id.rtrwnet.billing.config.BillingProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/** Kirim WhatsApp lewat gateway Fonnte (https://fonnte.com). */
@Component
@ConditionalOnProperty(name = "billing.whatsapp.provider", havingValue = "FONNTE")
public class FonnteWhatsAppSender implements WhatsAppSender {

    private final RestClient client;

    public FonnteWhatsAppSender(BillingProperties properties) {
        this.client = RestClient.builder()
                .baseUrl("https://api.fonnte.com")
                .defaultHeader("Authorization", properties.whatsapp().token())
                .build();
    }

    @Override
    public void send(String phone, String message) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("target", phone);
        form.add("message", message);
        client.post()
                .uri("/send")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
    }
}
