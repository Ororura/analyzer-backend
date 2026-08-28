package com.ororura.analyzer.vacancy.hh.dto;

import java.util.List;

import com.ororura.analyzer.vacancy.api.VacancyDtos.Salary;

public final class HhDtos {

    private HhDtos() {
    }

    public record SearchPage(List<SourceVacancy> items, Integer totalPages, boolean hasNext) {
        public SearchPage {
            items = List.copyOf(items);
        }
    }

    public record SourceVacancy(
            String id,
            String title,
            String company,
            String companyId,
            String url,
            String location,
            Salary salary,
            String description,
            List<String> skills,
            List<String> requirements,
            List<String> responsibilities,
            String experience,
            String employment,
            String schedule,
            String workFormat,
            String publishedAt) {

        public SourceVacancy {
            skills = List.copyOf(skills);
            requirements = List.copyOf(requirements);
            responsibilities = List.copyOf(responsibilities);
        }
    }
}
