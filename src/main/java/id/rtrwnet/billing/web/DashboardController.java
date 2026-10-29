package id.rtrwnet.billing.web;

import id.rtrwnet.billing.service.DashboardService;
import id.rtrwnet.billing.web.dto.DashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final Clock clock;

    @GetMapping
    public DashboardResponse summary(@RequestParam(required = false) YearMonth period) {
        return dashboardService.summary(period != null ? period : YearMonth.now(clock));
    }
}
