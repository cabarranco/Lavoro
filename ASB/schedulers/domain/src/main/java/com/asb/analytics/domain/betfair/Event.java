package com.asb.analytics.domain.betfair;

import com.asb.analytics.domain.SimpleItem;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Date;

/**
 * Class description
 *
 * @author Claudio Paolicelli
 */
@JsonIgnoreProperties
public class Event extends SimpleItem {

    private String countryCode;

    private String timezone;

    private Date openDate;

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Date getOpenDate() {
        return openDate;
    }

    public void setOpenDate(Date openDate) {
        this.openDate = openDate;
    }

    public String getTeam1() {

        return this.getName().split(" v ")[0].trim();
    }

    public String getTeam2() {

        return this.getName().split(" v ")[1].trim();
    }
}
