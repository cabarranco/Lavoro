package com.asbresearch.betfair.esa.protocol;

import java.util.EventListener;

public interface ConnectionStatusListener extends EventListener {
    void connectionStatusChange(ConnectionStatusChangeEvent statusChangeEvent);
}
