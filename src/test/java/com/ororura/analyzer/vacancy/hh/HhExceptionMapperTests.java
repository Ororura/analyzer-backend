package com.ororura.analyzer.vacancy.infrastructure.hh;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import com.ororura.analyzer.vacancy.application.port.VacancySourceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;

class HhExceptionMapperTests {

    @ParameterizedTest
    @CsvSource({
        "403, FORBIDDEN, 403, HH.ru отклонил запрос",
        "404, NOT_FOUND, 404, Страница вакансии не найдена"
    })
    void mapsClientErrors(int upstreamStatus, String code, int status, String message) {
        RuntimeException cause = new HttpClientErrorException(HttpStatus.valueOf(upstreamStatus));

        VacancySourceException result = HhExceptionMapper.map(cause);

        assertThat(result.getCode()).isEqualTo(code);
        assertThat(result.getStatus()).isEqualTo(status);
        assertThat(result.getMessage()).isEqualTo(message);
        assertThat(result.getCause()).isSameAs(cause);
    }

    @Test
    void preservesRetryAfterForRateLimit() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Retry-After", "30");
        RuntimeException cause = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", headers, new byte[0], StandardCharsets.UTF_8);

        VacancySourceException result = HhExceptionMapper.map(cause);

        assertThat(result.getCode()).isEqualTo("RATE_LIMITED");
        assertThat(result.getStatus()).isEqualTo(429);
        assertThat(result.getRetryAfter()).isEqualTo("30");
        assertThat(result.getCause()).isSameAs(cause);
    }

    @Test
    void mapsSocketTimeout() {
        RuntimeException cause = new ResourceAccessException("timeout", new SocketTimeoutException());

        VacancySourceException result = HhExceptionMapper.map(cause);

        assertThat(result.getCode()).isEqualTo("TIMEOUT");
        assertThat(result.getStatus()).isEqualTo(504);
        assertThat(result.getMessage()).isEqualTo("HH.ru не ответил вовремя");
        assertThat(result.getCause()).isSameAs(cause);
    }

    @Test
    void mapsUpstreamServerErrorToBadGateway() {
        RuntimeException cause = new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);

        VacancySourceException result = HhExceptionMapper.map(cause);

        assertThat(result.getCode()).isEqualTo("UPSTREAM");
        assertThat(result.getStatus()).isEqualTo(502);
        assertThat(result.getMessage()).isEqualTo("Временная ошибка HH.ru");
        assertThat(result.getCause()).isSameAs(cause);
    }
}
