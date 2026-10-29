package id.rtrwnet.billing.web;

import id.rtrwnet.billing.service.PlanService;
import id.rtrwnet.billing.web.dto.PlanRequest;
import id.rtrwnet.billing.web.dto.PlanResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @GetMapping
    public List<PlanResponse> list() {
        return planService.findAll().stream().map(PlanResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlanResponse create(@Valid @RequestBody PlanRequest request) {
        return PlanResponse.from(planService.create(request));
    }

    @PutMapping("/{id}")
    public PlanResponse update(@PathVariable Long id, @Valid @RequestBody PlanRequest request) {
        return PlanResponse.from(planService.update(id, request));
    }
}
