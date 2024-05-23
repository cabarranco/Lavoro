package com.asbresearch.betfair.esa.cache.util;

import lombok.Value;

@Value
public class RunnerId {
    private final long selectionId;
    private final Double handicap;

    public RunnerId(long selectionId, Double handicap) {
        this.selectionId = selectionId;
        this.handicap = handicap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RunnerId runnerId = (RunnerId) o;

        if (selectionId != runnerId.selectionId) return false;
        return handicap != null ? handicap.equals(runnerId.handicap) : runnerId.handicap == null;

    }

    @Override
    public int hashCode() {
        int result = (int) (selectionId ^ (selectionId >>> 32));
        result = 31 * result + (handicap != null ? handicap.hashCode() : 0);
        return result;
    }
}