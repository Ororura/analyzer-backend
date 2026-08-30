package com.ororura.analyzer.vacancy.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Salary(Integer from, Integer to, String currency, Boolean gross) implements Serializable {
}
