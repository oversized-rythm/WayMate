package com.waymate.backend.exception;


import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handle unexpected runtime errors without exposing internal details.
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String,String>>handleRuntimeException(RuntimeException exception){
        System.err.println("Unexpected error: " + exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message","An unexpected error occurred. Please try again later."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,String>>handleValidationErrors(
            MethodArgumentNotValidException exception){
        Map<String, String> errors= new HashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error->
                errors.put(error.getField(),error.getDefaultMessage())
        );

        return ResponseEntity.badRequest().body(errors);
    }

    //Handles custom application exceptions with their assigned Http Status
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handlesApiException(
            ApiException exception){

        //Return the status code and error message defined by the service.
        return ResponseEntity
                .status(exception.getStatus())
                .body(Map.of("message", exception.getMessage()));
    }
}
