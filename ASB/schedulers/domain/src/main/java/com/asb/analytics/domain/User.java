package com.asb.analytics.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * This is the user to access to the pulse UI
 *
 * @author Claudio Paolicelli
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public User() {
    }

    private String venue;
    private String username;
    private String password;
    private Boolean primary;
    private String type;
    private Boolean isActive;
    private String owner;
    private Double commissionRate;
    private Double balanceToSavePercentage;
    private String applicationKey;


    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getPrimary() {
        return primary;
    }

    public void setPrimary(Boolean primary) {
        this.primary = primary;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Double getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(Double commissionRate) {
        this.commissionRate = commissionRate;
    }

    public Double getBalanceToSavePercentage() {
        return balanceToSavePercentage;
    }

    public void setBalanceToSavePercentage(Double balanceToSavePercentage) {
        this.balanceToSavePercentage = balanceToSavePercentage;
    }

    public String getApplicationKey() {
        return applicationKey;
    }

    public void setApplicationKey(String applicationKey) {
        this.applicationKey = applicationKey;
    }
}
