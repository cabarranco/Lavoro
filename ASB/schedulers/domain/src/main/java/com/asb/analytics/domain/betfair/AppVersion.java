package com.asb.analytics.domain.betfair;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AppVersion {
    private String owner;
    private long versionId;
    private String version;
    private String applicationKey;
    private boolean delayData;
    private boolean subscriptionRequired;
    private boolean ownerManager;
    private boolean active;

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public long getVersionId() {
        return versionId;
    }

    public void setVersionId(long versionId) {
        this.versionId = versionId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getApplicationKey() {
        return applicationKey;
    }

    public void setApplicationKey(String applicationKey) {
        this.applicationKey = applicationKey;
    }

    public boolean isDelayData() {
        return delayData;
    }

    public void setDelayData(boolean delayData) {
        this.delayData = delayData;
    }

    public boolean isSubscriptionRequired() {
        return subscriptionRequired;
    }

    public void setSubscriptionRequired(boolean subscriptionRequired) {
        this.subscriptionRequired = subscriptionRequired;
    }

    public boolean isOwnerManager() {
        return ownerManager;
    }

    public void setOwnerManager(boolean ownerManager) {
        this.ownerManager = ownerManager;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}