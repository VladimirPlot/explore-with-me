package ru.practicum.ewm.exceptions;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.practicum.ewm.errors.ErrorResponse;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        return buildResponse(message, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({ConflictException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<ErrorResponse> buildResponse(String message, HttpStatus status) {
        return ResponseEntity.status(status).body(new ErrorResponse(message));
    }
}