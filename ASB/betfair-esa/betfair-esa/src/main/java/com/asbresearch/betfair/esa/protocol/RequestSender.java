package com.asbresearch.betfair.esa.protocol;

public interface RequestSender {

    void sendLine(String line) throws ConnectionException;
}
