package com.asbresearch.metrics.metrics.services;

import com.asbresearch.metrics.metrics.models.betfair.ClearedOrders;
import com.asbresearch.metrics.metrics.models.betfair.LoginResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;

@Service
@EnableConfigurationProperties()
public class BetfairRestService {

    private static final Logger log = LoggerFactory.getLogger(BetfairRestService.class);

    private final RestTemplate restTemplate;

    @Value("${betfair.betting.endpoint}")
    private String bettingUrl;

    @Value("${betfair.auth.endpoint}")
    private String authUrl;

    @Value("${betfair.username}")
    private String username;

    @Value("${betfair.password}")
    private String password;

    @Value("${betfair.app.key}")
    private String appKey;

    public BetfairRestService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public ClearedOrders getListClearedOrders(HashMap<String, Object> params) {

        String sessionToken = login();

        // create headers
        HttpHeaders headers = new HttpHeaders();
        // set `accept` header
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        // set custom header
        headers.set("X-Application", appKey);
        headers.set("X-Authentication", sessionToken);
        headers.set("Content-Type", "application/json");

        // build the request
        HttpEntity request = new HttpEntity<>(params, headers);


        String url = bettingUrl + "listClearedOrders/";
        ResponseEntity<ClearedOrders> response = this.restTemplate.postForEntity(
                url, request, ClearedOrders.class
        );

        if(response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        } else {
            return null;
        }
    }

    private String login() {

        // create headers
        HttpHeaders headers = new HttpHeaders();
        // set `accept` header
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        // set custom header
        headers.set("X-Application", appKey);
        headers.set("content-type", "application/x-www-form-urlencoded");

        // build the request
        HttpEntity request = new HttpEntity<>(String.format("username=%s&password=%s", username, password), headers);

        ResponseEntity<LoginResponse> response = this.restTemplate.postForEntity(
                authUrl, request, LoginResponse.class
        );

        String sessionToken = null;

        if(response.getBody() != null && response.getStatusCode() == HttpStatus.OK) {
            sessionToken = response.getBody().getToken();
        }

        log.info("Betfair session token " + sessionToken);
        return sessionToken;
    }
}
