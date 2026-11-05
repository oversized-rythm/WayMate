package com.waymate.backend.exception;


import org.springframework.http.HttpStatus;

//Custom exception used when the application needs to return
//a specific HTTP status code along with an error message
public class ApiException extends RuntimeException{
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message){
        super(message);
        this.status=status;
    }

    //Return the HTTP status associated with this exception

    public HttpStatus getStatus() {
        return status;
    }
}
