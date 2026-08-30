package com.ororura.analyzer.vacancy.hh;

import java.net.SocketTimeoutException;

import com.ororura.analyzer.vacancy.provider.VacancySourceException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

final class HhExceptionMapper {

    private HhExceptionMapper() {
    }

    static VacancySourceException map(RuntimeException exception) {
        if (exception instanceof VacancySourceException sourceException) return sourceException;
        if (exception instanceof RestClientResponseException responseException) {
            int status = responseException.getStatusCode().value();
            String retryAfter = responseException.getResponseHeaders() == null
                    ? null
                    : responseException.getResponseHeaders().getFirst("Retry-After");
            if (status == 403) return error("FORBIDDEN", "HH.ru отклонил запрос", 403, null, exception);
            if (status == 404) return error("NOT_FOUND", "Страница вакансии не найдена", 404, null, exception);
            if (status == 429) {
                return error("RATE_LIMITED", "HH.ru временно ограничил запросы", 429, retryAfter, exception);
            }
            if (status >= 500) return error("UPSTREAM", "Временная ошибка HH.ru", 502, null, exception);
            return error("UPSTREAM", "Неожиданный ответ HH.ru: " + status, 502, null, exception);
        }
        if (exception instanceof ResourceAccessException && hasCause(exception, SocketTimeoutException.class)) {
            return error("TIMEOUT", "HH.ru не ответил вовремя", 504, null, exception);
        }
        return error("UPSTREAM", "Не удалось подключиться к HH.ru", 502, null, exception);
    }

    static VacancySourceException error(
            String code, String message, int status, String retryAfter, Throwable cause) {
        return new VacancySourceException(code, message, status, retryAfter, cause);
    }

    static String detailReason(String code) {
        return switch (code) {
            case "TIMEOUT" -> "timeout";
            case "FORBIDDEN" -> "доступ отклонён";
            case "NOT_FOUND" -> "страница не найдена";
            case "RATE_LIMITED" -> "слишком много запросов";
            default -> "ошибка HH.ru";
        };
    }

    private static boolean hasCause(Throwable error, Class<? extends Throwable> type) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (type.isInstance(current)) return true;
        }
        return false;
    }
}
