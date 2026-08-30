package com.ororura.analyzer.vacancy.model;

import java.io.Serializable;

public record Pagination(int page, int pageSize, Integer totalPages, boolean hasNext) implements Serializable {
}
