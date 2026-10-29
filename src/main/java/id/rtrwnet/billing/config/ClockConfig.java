package id.rtrwnet.billing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    /** Semua perhitungan tanggal memakai zona WIB. Di test, bean ini bisa diganti jam tetap. */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Jakarta"));
    }
}
