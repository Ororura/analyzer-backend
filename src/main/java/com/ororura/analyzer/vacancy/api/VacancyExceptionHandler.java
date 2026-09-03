package com.ororura.analyzer.vacancy.api;

import com.ororura.analyzer.vacancy.api.VacancyController;
import com.ororura.analyzer.vacancy.application.port.VacancySourceException;
import com.ororura.analyzer.vacancy.application.search.InvalidVacancyQueryException;
import com.ororura.analyzer.vacancy.application.search.VacancyNotFoundException;
import com.ororura.analyzer.vacancy.application.selection.VacancySelectionException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {VacancyController.class, com.ororura.analyzer.market.api.VacancyMarketController.class})
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

    @ExceptionHandler(VacancySelectionException.class)
    public ResponseEntity<VacancyError> handleInvalidSelection(VacancySelectionException exception) {
        return ResponseEntity.badRequest().body(new VacancyError("INVALID_SELECTION", exception.getMessage()));
    }

    @ExceptionHandler(VacancyNotFoundException.class)
    public ResponseEntity<VacancyError> handleNotFound(VacancyNotFoundException exception) {
        return ResponseEntity.status(404).body(new VacancyError("NOT_FOUND", exception.getMessage()));
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
