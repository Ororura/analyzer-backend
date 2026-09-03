package com.ororura.analyzer.vacancy.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SalaryResponse(Integer from, Integer to, String currency, Boolean gross) {
}
