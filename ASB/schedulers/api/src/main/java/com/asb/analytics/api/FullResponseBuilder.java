package com.asb.analytics.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.util.Iterator;
import java.util.List;

class FullResponseBuilder {

    static String getFullResponse(HttpURLConnection con) throws Exception {

        StringBuilder fullResponseBuilder = new StringBuilder();

        fullResponseBuilder.append(con.getResponseCode())
                .append(" ")
                .append(con.getResponseMessage())
                .append("\n");

        con.getHeaderFields()
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey() != null)
                .forEach(entry -> {

                    fullResponseBuilder.append(entry.getKey())
                            .append(": ");

                    List<String> headerValues = entry.getValue();
                    Iterator<String> it = headerValues.iterator();
                    if (it.hasNext()) {
                        fullResponseBuilder.append(it.next());

                        while (it.hasNext()) {
                            fullResponseBuilder.append(", ")
                                    .append(it.next());
                        }
                    }

                    fullResponseBuilder.append("\n");
                });


        fullResponseBuilder.append("Response: ")
                .append(getContent(con));

        return fullResponseBuilder.toString();
    }

    static SimpleResponse getResponse(HttpURLConnection con) throws Exception {

        SimpleResponse response = new SimpleResponse();

        response.setCode(con.getResponseCode());
        response.setMessage(con.getResponseMessage());
        response.setHeaders(con.getHeaderFields());

        response.setBody(getContent(con).toString());

        return response;
    }

    private static StringBuilder getContent(HttpURLConnection con) throws Exception {
        Reader streamReader = null;

        if (con.getResponseCode() > 299) {
            streamReader = new InputStreamReader(con.getErrorStream());
        } else {
            streamReader = new InputStreamReader(con.getInputStream());
        }

        BufferedReader in = new BufferedReader(streamReader);
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }

        in.close();

        return content;
    }
}