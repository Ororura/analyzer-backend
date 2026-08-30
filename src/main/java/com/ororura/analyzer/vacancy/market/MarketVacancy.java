package com.ororura.analyzer.vacancy.market;

import java.util.List;

import com.ororura.analyzer.vacancy.model.Salary;
import com.ororura.analyzer.vacancy.model.Vacancy;

public record MarketVacancy(
        String id,
        String title,
        List<String> skills,
        List<String> requirements,
        List<String> responsibilities,
        String description,
        Salary salary,
        String experience,
        String employment,
        String schedule,
        String workFormat) {

    public MarketVacancy {
        skills = skills == null ? List.of() : List.copyOf(skills);
        requirements = requirements == null ? List.of() : List.copyOf(requirements);
        responsibilities = responsibilities == null ? List.of() : List.copyOf(responsibilities);
    }

    public MarketVacancy(List<String> skills, String experience, String employment, String schedule, String workFormat) {
        this(null, null, skills, List.of(), List.of(), null, null, experience, employment, schedule, workFormat);
    }

    public static MarketVacancy from(Vacancy vacancy) {
        return new MarketVacancy(vacancy.id(), vacancy.title(), vacancy.skills(), vacancy.requirements(),
                vacancy.responsibilities(), vacancy.description(), vacancy.salary(), vacancy.experience(),
                vacancy.employment(), vacancy.schedule(), vacancy.workFormat());
    }
}
