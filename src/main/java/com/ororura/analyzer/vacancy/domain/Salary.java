package com.ororura.analyzer.vacancy.domain;

import java.io.Serializable;

public record Salary(Integer from, Integer to, String currency, Boolean gross) implements Serializable {
}
