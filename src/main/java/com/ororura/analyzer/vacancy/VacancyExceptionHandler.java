package com.ororura.analyzer.vacancy;

import com.ororura.analyzer.vacancy.api.VacancyController;
import com.ororura.analyzer.vacancy.hh.VacancySourceException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static com.ororura.analyzer.vacancy.api.VacancyDtos.VacancyError;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = VacancyController.class)
public class VacancyExceptionHandler {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<VacancyError> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.badRequest()
                .body(new VacancyError("INVALID_QUERY", "Числовой параметр имеет неверный формат"));
    }

    @ExceptionHandler(InvalidVacancyQueryException.class)
    public ResponseEntity<VacancyError> handleInvalidQuery(InvalidVacancyQueryException exception) {
        return ResponseEntity.badRequest().body(new VacancyError("INVALID_QUERY", exception.getMessage()));
    }

    @ExceptionHandler(VacancySourceException.class)
    public ResponseEntity<VacancyError> handleUpstream(VacancySourceException exception) {
        ResponseEntity.BodyBuilder response = ResponseEntity.status(exception.getStatus());
        if (exception.getRetryAfter() != null) {
            response.header("Retry-After", exception.getRetryAfter());
        }
        return response.body(new VacancyError(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<VacancyError> handleUnexpected(Exception exception) {
        return ResponseEntity.internalServerError()
                .body(new VacancyError("INTERNAL", "Не удалось загрузить вакансии"));
    }
}
