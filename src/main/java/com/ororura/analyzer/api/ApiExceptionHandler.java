package com.ororura.analyzer.api;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import com.ororura.analyzer.resume.error.ResumeAnalysisException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResumeAnalysisException.class)
    public ResponseEntity<ApiErrorResponse> handleResumeAnalysis(ResumeAnalysisException exception) {
        return ResponseEntity.status(exception.getCode().status())
                .body(ApiErrorResponse.of(exception.getCode().name(), exception.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleUploadTooLarge(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                .body(ApiErrorResponse.of("PDF_TOO_LARGE", "PDF file exceeds the configured size limit"));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingPart(MissingServletRequestPartException exception) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of("INVALID_FILE", "PDF file is required and must not be empty"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiErrorResponse.of("UNSUPPORTED_FILE_TYPE", "A multipart PDF request is required"));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedMultipart(MultipartException exception) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.of("INVALID_FILE", "Invalid multipart request"));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse("Invalid request");
        return ResponseEntity.badRequest().body(ApiErrorResponse.of("INVALID_QUERY", message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.of("INVALID_QUERY", "Invalid request parameter"));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NoHandlerFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of("NOT_FOUND", "Route not found"));
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleStatus(org.springframework.web.server.ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(ApiErrorResponse.of("REQUEST_REJECTED", e.getReason()));
    }

    @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class})
    public ResponseEntity<ApiErrorResponse> handleBody(Exception e) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.of("INVALID_BODY", "Invalid request body"));
    }

    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleVersion(Exception e) {
        return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                .body(ApiErrorResponse.of("VERSION_CONFLICT", "Profile version has changed"));
    }

    @ExceptionHandler(com.ororura.analyzer.analysis.application.ProfileAnalysisService.ProfileAnalysisFailure.class)
    public ResponseEntity<ApiErrorResponse> handleRunFailure(
            com.ororura.analyzer.analysis.application.ProfileAnalysisService.ProfileAnalysisFailure e) {
        var response = e.getCause() instanceof ResumeAnalysisException failure ? handleResumeAnalysis(failure) :
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiErrorResponse.of("ANALYSIS_FAILED", "Analysis failed"));
        return ResponseEntity.status(response.getStatusCode()).header("X-Analysis-Run-Id",e.runId().toString())
                .body(response.getBody());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of("INTERNAL_ERROR", "Internal server error"));
    }
}
