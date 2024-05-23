package com.asbresearch.betfair.ref.exceptions;

public class LoginException extends Throwable {

    public LoginException(Exception ex) {
        super(ex.getMessage());
    }
}
