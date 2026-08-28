package com.ororura.analyzer.vacancy.api;

import java.util.List;

import com.ororura.analyzer.vacancy.VacancySearchQuery;
import com.ororura.analyzer.vacancy.VacancyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.ororura.analyzer.vacancy.api.VacancyDtos.VacancySearchResult;

@RestController
@RequestMapping("/api")
public class VacancyController {

    private final VacancyService service;

    public VacancyController(VacancyService service) {
        this.service = service;
    }

    @GetMapping("/vacancies")
    public VacancySearchResult vacancies(
            @RequestParam(required = false) String text,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int perPage,
            @RequestParam(required = false) List<String> experience,
            @RequestParam(required = false) List<String> employment,
            @RequestParam(required = false) List<String> schedule,
            @RequestParam(required = false) Integer salary,
            @RequestParam(required = false) String location) {
        return service.search(new VacancySearchQuery(
                text, page, perPage, experience, employment, schedule, salary, location));
    }
}
