package com.asb.analytics.api;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class HttpConnector {

    // Methods
    public static final String POST = "POST";
    public static final String GET = "GET";

    private final String url;
    private int readTimeout = 5000;
    private int connectionTimeout = 5000;
    private String rawData = null;
    private HashMap<String, String> headers = new HashMap<>();
    private String method = GET;

    private HttpConnector(String url) {
        this.url = url;
    }

    public static HttpConnector connect(String url) {
        return new HttpConnector(url);
    }

    public HttpConnector timeout(int timeout) {
        this.readTimeout = timeout;
        this.connectionTimeout = timeout;

        return this;
    }

    public HttpConnector header(String key, String value) {
        this.headers.put(key, value);

        return this;
    }

    public HttpConnector body(String rawData) {
        this.rawData = rawData;

        return this;
    }

    public HttpConnector method(String method) {
        this.method = method;

        return this;
    }

    public SimpleResponse execute() throws Exception {

        URL url = new URL(this.url);

        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod(method);

        headers.forEach(con::setRequestProperty);

        con.setConnectTimeout(readTimeout);
        con.setReadTimeout(connectionTimeout);
        con.setDoOutput(true);

        if (POST.equalsIgnoreCase(method) && rawData != null) {
            try(OutputStream os = con.getOutputStream()) {
                byte[] input = rawData.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        }


        return FullResponseBuilder.getResponse(con);
    }
}
