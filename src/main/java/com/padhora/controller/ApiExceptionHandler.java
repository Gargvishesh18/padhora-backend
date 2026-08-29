package com.padhora.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

// Without this, a failed @Valid check (e.g. a missing/oversized field) returns Spring's default
// error shape, which the frontend's `data.error` never finds - so real validation messages like
// "Your name is required" silently turned into a generic "Could not send your request."
// This translates them into the {error: "..."} shape every controller here already uses.
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage())
                .orElse("Please check the form and try again.");
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    // Safety net: anything that still reaches the DB layer with a bad value (e.g. a constraint
    // this version of the code doesn't validate yet) returns a clean 400 instead of a raw 500.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of("error", "That request couldn't be saved - please check the details and try again."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneric(Exception ex) {
        log.error("Unhandled error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Something went wrong on our end. Please try again."));
    }
}
