package com.ororura.analyzer.vacancy.market;

import java.util.List;

public record MarketVacancy(
        List<String> skills,
        String experience,
        String employment,
        String schedule,
        String workFormat) {

    public MarketVacancy {
        skills = skills == null ? List.of() : List.copyOf(skills);
    }
}
