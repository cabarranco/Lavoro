package com.asbresearch.betfair.esa.auth;

import java.time.Clock;
import java.time.Instant;

public class AppKeyAndSession {

    private String appkey;
    private String session;
    private Instant createTime;


    public AppKeyAndSession(String appkey, String session) {
        this.appkey = appkey;
        this.session = session;
        createTime= Instant.now(Clock.systemUTC());
    }

    public Instant getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Instant createTime) {
        this.createTime = createTime;
    }

    public String getSession() {
        return session;
    }

    public String getAppkey() {
        return appkey;
    }
}
