package com.asbresearch.betfair.esa.protocol;

import com.betfair.esa.swagger.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.asbresearch.betfair.esa.Client.MARKET_CHANGE_ID;
import static com.asbresearch.betfair.esa.Client.START_TIME;

@Slf4j
public class RequestResponseProcessor {
    public static final String REQUEST_AUTHENTICATION = "authentication";
    public static final String REQUEST_MARKET_SUBSCRIPTION = "marketSubscription";
    public static final String REQUEST_ORDER_SUBSCRIPTION = "orderSubscription";
    public static final String REQUEST_HEARTBEAT = "heartbeat";

    public static final String RESPONSE_CONNECTION = "connection";
    public static final String RESPONSE_STATUS = "status";
    public static final String RESPONSE_MARKET_CHANGE_MESSAGE = "mcm";
    public static final String RESPONSE_ORDER_CHANGE_MESSAGE = "ocm";

    private final ObjectMapper objectMapper;
    private final AtomicInteger nextId = new AtomicInteger();
    private FutureResponse<ConnectionMessage> connectionMessage = new FutureResponse<>();
    private ConcurrentHashMap<Integer, RequestResponse> tasks = new ConcurrentHashMap<>();

    //subscription handlers
    private SubscriptionHandler<MarketSubscriptionMessage, ChangeMessage<MarketChange>, MarketChange> marketSubscriptionHandler;
    private SubscriptionHandler<OrderSubscriptionMessage, ChangeMessage<OrderMarketChange>, OrderMarketChange> orderSubscriptionHandler;

    private ChangeMessageHandler changeHandler;
    private final RequestSender sendLine;
    private ConnectionStatus status = ConnectionStatus.STOPPED;
    private final CopyOnWriteArrayList<ConnectionStatusListener> connectionStatusListeners = new CopyOnWriteArrayList<>();

    private long lastRequestTime = Long.MAX_VALUE;
    private long lastResponseTime = Long.MAX_VALUE;

    private int traceChangeTruncation;
    private final Object sendLock = new Object();

    public RequestResponseProcessor(RequestSender sendLine) {
        this.sendLine = sendLine;
        setChangeHandler(null);

        objectMapper = new ObjectMapper();
        objectMapper.addMixIn(ResponseMessage.class, MixInResponseMessage.class);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private void setStatus(ConnectionStatus value) {
        if (value == status) {
            //no-op
            return;
        }
        ConnectionStatusChangeEvent args = new ConnectionStatusChangeEvent(this, status, value);
        log.info("ESAClient: Status changed {} -> {}", status, value);
        status = value;

        dispatchConnectionStatusChange(args);
    }

    public ConnectionStatus getStatus() {
        return status;
    }


    private void dispatchConnectionStatusChange(ConnectionStatusChangeEvent args) {
        try {
            connectionStatusListeners.forEach(c -> c.connectionStatusChange(args));
        } catch (Exception e) {
            log.error("Exception during event dispatch", e);
        }
    }

    public void addConnectionStatusListener(ConnectionStatusListener listener) {
        connectionStatusListeners.add(listener);
    }

    public void removeConnectionStatusListener(ConnectionStatusListener listener) {
        connectionStatusListeners.remove(listener);
    }

    public int getTraceChangeTruncation() {
        return traceChangeTruncation;
    }

    public void setTraceChangeTruncation(int traceChangeTruncation) {
        this.traceChangeTruncation = traceChangeTruncation;
    }

    public long getLastRequestTime() {
        return lastRequestTime;
    }

    public long getLastResponseTime() {
        return lastResponseTime;
    }

    private void reset() {
        ConnectionException cancelException = new ConnectionException("Connection reset - task cancelled");
        connectionMessage.setException(cancelException);
        connectionMessage = new FutureResponse<>();
        for (RequestResponse task : tasks.values()) {
            task.getFuture().setException(cancelException);
        }
        tasks = new ConcurrentHashMap<>();
    }

    public void disconnected() {
        setStatus(ConnectionStatus.DISCONNECTED);
        reset();
    }

    public void stopped() {
        marketSubscriptionHandler = null;
        orderSubscriptionHandler = null;
        setStatus(ConnectionStatus.STOPPED);
        reset();
    }

    public MarketSubscriptionMessage getMarketResubscribeMessage() {
        if (marketSubscriptionHandler != null) {
            MarketSubscriptionMessage resub = marketSubscriptionHandler.getSubscriptionMessage();
            resub.setInitialClk(marketSubscriptionHandler.getInitialClk());
            resub.setClk(marketSubscriptionHandler.getClk());
            return resub;
        }
        return null;
    }

    public OrderSubscriptionMessage getOrderResubscribeMessage() {
        if (orderSubscriptionHandler != null) {
            OrderSubscriptionMessage resub = orderSubscriptionHandler.getSubscriptionMessage();
            resub.setInitialClk(orderSubscriptionHandler.getInitialClk());
            resub.setClk(orderSubscriptionHandler.getClk());
            return resub;
        }
        return null;
    }

    public ChangeMessageHandler getChangeHandler() {
        return changeHandler;
    }

    public void setChangeHandler(ChangeMessageHandler changeHandler) {
        if (changeHandler == null) changeHandler = new NullChangeHandler();
        this.changeHandler = changeHandler;
    }

    public SubscriptionHandler<MarketSubscriptionMessage, ChangeMessage<MarketChange>, MarketChange> getMarketSubscriptionHandler() {
        return marketSubscriptionHandler;
    }

    public void setMarketSubscriptionHandler(SubscriptionHandler<MarketSubscriptionMessage, ChangeMessage<MarketChange>, MarketChange> newHandler) {
        if (marketSubscriptionHandler != null) marketSubscriptionHandler.cancel();
        marketSubscriptionHandler = newHandler;
        if (marketSubscriptionHandler != null) setStatus(ConnectionStatus.SUBSCRIBED);
    }

    public SubscriptionHandler<OrderSubscriptionMessage, ChangeMessage<OrderMarketChange>, OrderMarketChange> getOrderSubscriptionHandler() {
        return orderSubscriptionHandler;
    }

    public void setOrderSubscriptionHandler(SubscriptionHandler<OrderSubscriptionMessage, ChangeMessage<OrderMarketChange>, OrderMarketChange> newHandler) {
        if (orderSubscriptionHandler != null) orderSubscriptionHandler.cancel();
        orderSubscriptionHandler = newHandler;
        if (orderSubscriptionHandler != null) setStatus(ConnectionStatus.SUBSCRIBED);
    }

    public FutureResponse<ConnectionMessage> getConnectionMessage() {
        return connectionMessage;
    }

    public FutureResponse<StatusMessage> authenticate(AuthenticationMessage message) throws ConnectionException {
        header(message, REQUEST_AUTHENTICATION);
        return sendMessage(message, success -> setStatus(ConnectionStatus.AUTHENTICATED));
    }

    public FutureResponse<StatusMessage> heartbeat(HeartbeatMessage message) throws ConnectionException {
        header(message, REQUEST_HEARTBEAT);
        return sendMessage(message, null);
    }

    public FutureResponse<StatusMessage> marketSubscription(MarketSubscriptionMessage message) throws ConnectionException {
        header(message, REQUEST_MARKET_SUBSCRIPTION);
        return sendMessage(message, success -> setMarketSubscriptionHandler(new SubscriptionHandler<>(message, false)));
    }

    public FutureResponse<StatusMessage> orderSubscription(OrderSubscriptionMessage message) throws ConnectionException {
        header(message, REQUEST_ORDER_SUBSCRIPTION);
        return sendMessage(message, success -> setOrderSubscriptionHandler(new SubscriptionHandler<>(message, false)));
    }

    private FutureResponse<StatusMessage> sendMessage(RequestMessage message, Consumer<RequestResponse> onSuccess) throws ConnectionException {
        synchronized (sendLock) {
            int id = message.getId();
            RequestResponse requestResponse = new RequestResponse(id, message, onSuccess);
            //store a future task
            tasks.put(id, requestResponse);
            //serialize message & send
            String line;
            try {
                line = objectMapper.writeValueAsString(message);
            } catch (JsonProcessingException e) {
                //should never happen
                throw new ConnectionException("Failed to marshall json", e);
            }
            log.info("Client->ESA: " + line);
            //send line
            sendLine.sendLine(line);
            //time
            lastRequestTime = System.currentTimeMillis();
            return requestResponse.getFuture();
        }
    }

    private int header(RequestMessage msg, String op) {
        int id = nextId.incrementAndGet();
        msg.setId(id);
        msg.setOp(op);
        return id;
    }

    public ResponseMessage receiveLine(String line) throws IOException {
        lastResponseTime = Instant.now().toEpochMilli();
        ResponseMessage message = objectMapper.readValue(line, ResponseMessage.class);
        switch (message.getOp()) {
            case RESPONSE_CONNECTION:
                log.info("ESA->Client: {}", line);
                processConnectionMessage((ConnectionMessage) message);
                break;
            case RESPONSE_STATUS:
                log.info("ESA->Client: {}", line);
                processStatusMessage((StatusMessage) message);
                break;
            case RESPONSE_MARKET_CHANGE_MESSAGE:
                try {
                    String mkChangeId = UUID.randomUUID().toString().replace("-", "");
                    log.debug("ESA->Client: mkChangeId={} startTime={} {}", mkChangeId, lastResponseTime, line);
                    MDC.put(MARKET_CHANGE_ID, mkChangeId);
                    MDC.put(START_TIME, String.valueOf(lastResponseTime));
                    traceChange(line);
                    processMarketChangeMessage((MarketChangeMessage) message);
                } catch (RuntimeException ex) {
                    log.error("RuntimeException while processing {}", line, ex);
                } finally {
                    MDC.clear();
                }
                break;
            case RESPONSE_ORDER_CHANGE_MESSAGE:
                traceChange(line);
                processOrderChangeMessage((OrderChangeMessage) message);
                break;
            default:
                log.error("ESA->Client: Unknown message type: {}, message:{}", message.getOp(), line);
                break;
        }
        return message;

    }

    private void traceChange(String line) {
        if (traceChangeTruncation != 0) {
            log.info("ESA->Client: {}", line.substring(0, Math.min(traceChangeTruncation, line.length())));
        }
    }

    private void processOrderChangeMessage(OrderChangeMessage message) {
        ChangeMessage<OrderMarketChange> change = ChangeMessageFactory.toChangeMessage(message);
        change = orderSubscriptionHandler.processChangeMessage(change);

        if (change != null) changeHandler.onOrderChange(change);
    }

    private void processMarketChangeMessage(MarketChangeMessage message) {
        ChangeMessage<MarketChange> change = ChangeMessageFactory.toChangeMessage(message);
        change = marketSubscriptionHandler.processChangeMessage(change);
        if (change != null) changeHandler.onMarketChange(change);
    }

    private void processStatusMessage(StatusMessage statusMessage) {
        if (statusMessage.getId() == null) {
            //async status / status for a message that couldn't be decoded
            processUncorrelatedStatus(statusMessage);
        } else {
            RequestResponse task = tasks.get(statusMessage.getId());
            if (task == null) {
                //shouldn't happen
                processUncorrelatedStatus(statusMessage);
            } else {
                //unwind task
                task.processStatusMessage(statusMessage);
            }
        }
    }

    private void processUncorrelatedStatus(StatusMessage statusMessage) {
        log.error("Error Status Notification: {}", statusMessage);
        changeHandler.onErrorStatusNotification(statusMessage);
    }

    private void processConnectionMessage(ConnectionMessage message) {
        connectionMessage.setResponse(message);
        setStatus(ConnectionStatus.CONNECTED);
    }

    public static class NullChangeHandler implements ChangeMessageHandler {
        @Override
        public void onOrderChange(ChangeMessage<OrderMarketChange> change) {
            log.info("onOrderChange: " + change);
        }

        @Override
        public void onMarketChange(ChangeMessage<MarketChange> change) {
            log.info("onMarketChange: " + change);
        }

        @Override
        public void onErrorStatusNotification(StatusMessage message) {
            log.info("onErrorStatusNotification: " + message);
        }
    }
}
