package com.asbresearch.pulse.service.plm;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class ConcentrationRecords {
    private final Map<String, ConcentrationRecord> records = new HashMap<>();

    public ConcentrationRecord upSert(String id, double maxAvailableBalanceToBet, double usedBalance) {
        ConcentrationRecord concentrationRecord = records.getOrDefault(id, ConcentrationRecord.of(id, 0.0, 0.0));
        concentrationRecord = concentrationRecord.updateBalance(maxAvailableBalanceToBet, usedBalance);
        records.put(id, concentrationRecord);
        return concentrationRecord;
    }

    public ConcentrationRecord get(String id) {
        return records.get(id);
    }

    @Override
    public String toString() {
        return String.format("ConcentrationRecords %s", records);
    }

    public Map<String, ConcentrationRecord> getRecords() {
        return Collections.unmodifiableMap(records);
    }
}
