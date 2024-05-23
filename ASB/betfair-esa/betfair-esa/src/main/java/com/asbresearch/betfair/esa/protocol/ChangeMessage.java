package com.asbresearch.betfair.esa.protocol;

import java.time.Instant;
import java.util.List;
import lombok.Data;

@Data
public class ChangeMessage<T> {
    private Instant arrivalTime;
    private long publishTime;
    private int id;
    private String clk;
    private String initialClk;
    private Long heartbeatMs;
    private Long conflateMs;
    private List<T> items;
    private SegmentType segmentType;
    private ChangeType changeType;

    public ChangeMessage() {
        arrivalTime = Instant.now();
    }

    /**
     * Start of new subscription (not resubscription)
     * @return
     */
    public boolean isStartOfNewSubscription(){
        return changeType == ChangeType.SUB_IMAGE &&
                (segmentType == SegmentType.NONE || segmentType == SegmentType.SEG_START);
    }

    /**
     * Start of subscription / resubscription
     * @return
     */
    public boolean isStartOfRecovery(){
        return (changeType == ChangeType.SUB_IMAGE || changeType == ChangeType.RESUB_DELTA) &&
                (segmentType == SegmentType.NONE || segmentType == SegmentType.SEG_START);
    }

    /**
     * End of subscription / resubscription
     * @return
     */
    public boolean isEndOfRecovery(){
        return (changeType == ChangeType.SUB_IMAGE || changeType == ChangeType.RESUB_DELTA) &&
                (segmentType == SegmentType.NONE || segmentType == SegmentType.SEG_END);
    }
}
