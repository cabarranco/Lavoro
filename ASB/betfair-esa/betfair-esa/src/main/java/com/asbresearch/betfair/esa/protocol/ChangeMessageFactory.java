package com.asbresearch.betfair.esa.protocol;

import com.betfair.esa.swagger.model.MarketChange;
import com.betfair.esa.swagger.model.MarketChangeMessage;
import com.betfair.esa.swagger.model.OrderChangeMessage;
import com.betfair.esa.swagger.model.OrderMarketChange;

public class ChangeMessageFactory {

    public static ChangeMessage<MarketChange> toChangeMessage(MarketChangeMessage message) {
        ChangeMessage<MarketChange> change = new ChangeMessage<>();
        change.setId(message.getId());
        change.setPublishTime(message.getPt());
        change.setClk(message.getClk());
        change.setInitialClk(message.getInitialClk());
        change.setConflateMs(message.getConflateMs());
        change.setHeartbeatMs(message.getHeartbeatMs());
        change.setItems(message.getMc());
        SegmentType segmentType = SegmentType.NONE;
        if (message.getSegmentType() != null) {
            switch (message.getSegmentType()) {
                case SEG_START:
                    segmentType = SegmentType.SEG_START;
                    break;
                case SEG_END:
                    segmentType = SegmentType.SEG_END;
                    break;
                case SEG:
                    segmentType = SegmentType.SEG;
                    break;
            }
        }
        change.setSegmentType(segmentType);
        ChangeType changeType = ChangeType.UPDATE;
        if (message.getCt() != null) {
            switch (message.getCt()) {
                case HEARTBEAT:
                    changeType = ChangeType.HEARTBEAT;
                    break;
                case RESUB_DELTA:
                    changeType = ChangeType.RESUB_DELTA;
                    break;
                case SUB_IMAGE:
                    changeType = ChangeType.SUB_IMAGE;
                    break;
            }
        }
        change.setChangeType(changeType);
        return change;
    }

    public static ChangeMessage<OrderMarketChange> toChangeMessage(OrderChangeMessage message) {
        ChangeMessage<OrderMarketChange> change = new ChangeMessage<>();
        change.setId(message.getId());
        change.setPublishTime(message.getPt());
        change.setClk(message.getClk());
        change.setInitialClk(message.getInitialClk());
        change.setConflateMs(message.getConflateMs());
        change.setHeartbeatMs(message.getHeartbeatMs());


        change.setItems(message.getOc());

        SegmentType segmentType = SegmentType.NONE;
        if (message.getSegmentType() != null) {
            switch (message.getSegmentType()) {
                case SEG_START:
                    segmentType = SegmentType.SEG_START;
                    break;
                case SEG_END:
                    segmentType = SegmentType.SEG_END;
                    break;
                case SEG:
                    segmentType = SegmentType.SEG;
                    break;
            }
        }
        change.setSegmentType(segmentType);

        ChangeType changeType = ChangeType.UPDATE;
        if (message.getCt() != null) {
            switch (message.getCt()) {
                case HEARTBEAT:
                    changeType = ChangeType.HEARTBEAT;
                    break;
                case RESUB_DELTA:
                    changeType = ChangeType.RESUB_DELTA;
                    break;
                case SUB_IMAGE:
                    changeType = ChangeType.SUB_IMAGE;
                    break;
            }
        }
        change.setChangeType(changeType);

        return change;
    }
}
