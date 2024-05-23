package com.asbresearch.betfair.ref;

import com.asbresearch.betfair.ref.enums.Endpoint;
import com.asbresearch.betfair.ref.enums.Exchange;
import com.asbresearch.betfair.ref.util.Helpers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;


@Slf4j
public class Network {
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private final String appKey;
    private final String sessionToken;
    private final ObjectMapper mapper;

    public Network(String appKey, String sessionToken, ObjectMapper mapper) {
        this.appKey = appKey;
        this.sessionToken = sessionToken;
        this.mapper = mapper;
    }

    public <T> BetfairServerResponse<T> invoke(JavaType resultType,
                                               Exchange exchange,
                                               Endpoint endpoint,
                                               String method,
                                               Map<String, Object> args) {
        if (Helpers.isNullOrWhitespace(method)) throw new IllegalArgumentException(method);
        log.debug("{}, {}", formatEndpoint(endpoint), method);

        Instant requestStart = Instant.now();
        long requestStartMillis = System.currentTimeMillis();

        String url;
        if (exchange == Exchange.AUS) {
            url = "https://api-au.betfair.com/exchange";
        } else {
            url = "https://api.betfair.com/exchange";
        }

        if (endpoint == Endpoint.Betting) {
            url += "/betting/json-rpc/v1";
        } else {
            url += "/account/json-rpc/v1";
        }

        String requestData = null;
        JsonRequest call = null;
        try {
            call = JsonRequest.of(method, args);
            requestData = mapper.writeValueAsString(call);
        } catch (JsonProcessingException e) {
            log.error("Request serialization error: {}", call, e);
            throw new RuntimeException(e);
        }
        log.info("method={} requestJson={}", method, requestData);
        String result = requestSync(url, requestData, ContentType.APPLICATION_JSON, "application/json", appKey, sessionToken);
        log.info("method={} responseJson={}", method, result);

        JsonResponse<T> entity;
        try {
            entity = mapper.readValue(result, mapper.getTypeFactory().constructParametricType(JsonResponse.class, resultType));
        } catch (JsonProcessingException e) {
            log.error("Response deserialization error: {}", result, e);
            throw new RuntimeException(e);
        }
        if (entity != null) {
            BetfairServerResponse<T> response = new BetfairServerResponse<T>(
                    entity.getResult(),
                    Instant.now(),
                    requestStart,
                    (System.currentTimeMillis() - requestStartMillis) / 1000,
                    entity.getHasError());
            return response;
        } else
            return new BetfairServerResponse<>(
                    null,
                    Instant.now(),
                    requestStart,
                    (System.currentTimeMillis() - requestStartMillis) / 1000,
                    true);
    }

    public BetfairServerResponse<KeepAliveResponse> keepAliveSynchronous() {
        Instant requestStart = Instant.now();
        long requestStartMillis = System.currentTimeMillis();

        String keepAliveResponse = this.requestSync(
                "https://identitysso.betfair.com/api/keepAlive",
                "",
                ContentType.APPLICATION_FORM_URLENCODED,
                "application/json",
                this.appKey,
                this.sessionToken);

        KeepAliveResponse entity = null;
        try {
            entity = mapper.readValue(keepAliveResponse, KeepAliveResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Error Deserialize: {} to KeepAliveResponse", keepAliveResponse, e);
            throw new RuntimeException(e);
        }
        if (entity != null) {
            BetfairServerResponse<KeepAliveResponse> response = new BetfairServerResponse<>(
                    entity,
                    Instant.now(),
                    requestStart,
                    (System.currentTimeMillis() - requestStartMillis) / 1000,
                    Boolean.parseBoolean(entity.getError()));
            return response;
        } else {
            KeepAliveResponse response = KeepAliveResponse.error("Keep Alive request failed.");
            return new BetfairServerResponse<>(
                    response,
                    Instant.now(),
                    requestStart,
                    (System.currentTimeMillis() - requestStartMillis) / 1000,
                    true);
        }
    }

    private String requestSync(
            String url,
            String requestPostData,
            ContentType contentType,
            String acceptType,
            String appKey,
            String sessionToken) {
        Header[] headers = {
                new BasicHeader("X-Application", appKey),
                new BasicHeader("X-Authentication", sessionToken),
                new BasicHeader("Cache-Control", "no-cache"),
                new BasicHeader("Pragma", "no-cache"),
                new BasicHeader("Accept", acceptType)
        };

        CloseableHttpClient client = HttpClientBuilder.create()
                .setDefaultHeaders(new ArrayList<>(Arrays.asList(headers)))
                .build();
        try {
            StringEntity entity = new StringEntity(requestPostData);
            entity.setContentType(contentType.toString());
            HttpPost post = new HttpPost(url);
            post.setEntity(entity);
            HttpResponse response = client.execute(post);

            String json = EntityUtils.toString(response.getEntity(), "UTF-8");
            return json;
        } catch (IOException exception) {
            return null;
        }
    }

    private String formatEndpoint(Endpoint endpoint) {
        return endpoint == Endpoint.Betting ? "betting" : "account";
    }
}
