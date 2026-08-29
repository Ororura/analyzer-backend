package com.ororura.analyzer.vacancy.api;

import java.util.List;
import java.time.LocalDate;

import com.ororura.analyzer.vacancy.VacancySearchCriteria;
import com.ororura.analyzer.vacancy.VacancyService;
import com.ororura.analyzer.vacancy.VacancySort;
import com.ororura.analyzer.vacancy.WorkFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.ororura.analyzer.vacancy.api.VacancyDtos.VacancySearchResult;
import static com.ororura.analyzer.vacancy.api.VacancyDtos.Vacancy;

@RestController
@RequestMapping("/api")
public class VacancyController {

    private final VacancyService service;

    public VacancyController(VacancyService service) {
        this.service = service;
    }

    @GetMapping("/vacancies")
    public VacancySearchResult vacancies(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String text,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String employer,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) WorkFormat workFormat,
            @RequestParam(required = false) Integer salaryFrom,
            @RequestParam(required = false) Integer salaryTo,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) Boolean salaryOnly,
            @RequestParam(required = false) List<String> technologies,
            @RequestParam(required = false) LocalDate publishedFrom,
            @RequestParam(required = false) VacancySort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer perPage,
            @RequestParam(required = false) List<String> experience,
            @RequestParam(required = false) List<String> employment,
            @RequestParam(required = false) List<String> schedule,
            @RequestParam(required = false) Integer salary,
            @RequestParam(required = false) Boolean onlyWithSalary) {
        int resolvedPageSize = pageSize != null ? pageSize : perPage != null ? perPage : 20;
        return service.search(new VacancySearchCriteria(
                query != null ? query : text, title, area != null ? area : location, employer,
                experience, level, workFormat, salaryFrom != null ? salaryFrom : salary, salaryTo, currency,
                salaryOnly != null ? salaryOnly : onlyWithSalary, technologies, publishedFrom, sort,
                page, resolvedPageSize, employment, schedule));
    }

    @GetMapping("/vacancies/{id}")
    public Vacancy vacancy(@org.springframework.web.bind.annotation.PathVariable String id) {
        return service.getById(id);
    }
}
