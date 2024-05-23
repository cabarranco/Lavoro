package com.asbresearch.betfair.esa.protocol;

import com.betfair.esa.swagger.model.StatusMessage;

public class StatusException extends Exception {

    private final StatusMessage.ErrorCodeEnum errorCode;
    private final String errorMessage;

    public StatusException(StatusMessage message)
    {
        super(message.getErrorCode() +": " +message.getErrorMessage());
        errorCode = message.getErrorCode();
        errorMessage = message.getErrorMessage();
    }

    public StatusMessage.ErrorCodeEnum getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
