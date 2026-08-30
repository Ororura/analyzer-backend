package com.ororura.analyzer.vacancy.api;

import com.ororura.analyzer.vacancy.market.VacancyMarketData;
import com.ororura.analyzer.vacancy.market.VacancyMarketService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api")
public class VacancyMarketController {

    private final VacancyMarketService service;

    public VacancyMarketController(VacancyMarketService service) {
        this.service = service;
    }

    @GetMapping("/vacancy-market")
    public VacancyMarketData vacancyMarket(
            @RequestParam(defaultValue = "Java Backend Developer") @NotBlank @Size(max = 200) String text) {
        return service.load(text.trim());
    }
}
