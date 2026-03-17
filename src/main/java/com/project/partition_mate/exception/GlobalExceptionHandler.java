package com.project.partition_mate.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {

        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("요청값이 올바르지 않습니다.");

        ErrorResponse response = new ErrorResponse("VALIDATION_ERROR", errorMessage);


        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);

    }

    @ExceptionHandler(CustomAuthException.class)
    public ResponseEntity<ErrorResponse> handleCustomAuthException(CustomAuthException ex) {

        HttpStatus status = ex.getHttpStatus();
        String errorMessage = ex.getMessage();

        ErrorResponse response = new ErrorResponse(ex.getErrorCode(), errorMessage);
        return new ResponseEntity<>(response, status);


    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException ex) {

        HttpStatus status = HttpStatus.NOT_FOUND;
        String errorMessage = ex.getMessage();

        ErrorResponse response = new ErrorResponse("ENTITY_NOT_FOUND", errorMessage);
        return new ResponseEntity<>(response, status);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        HttpStatus status = ex.getStatus();
        String errorMessage = ex.getMessage();
        ErrorResponse response = new ErrorResponse("BUSINESS_ERROR", errorMessage);
        return new ResponseEntity<>(response, status);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse response = new ErrorResponse("INVALID_REQUEST", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
