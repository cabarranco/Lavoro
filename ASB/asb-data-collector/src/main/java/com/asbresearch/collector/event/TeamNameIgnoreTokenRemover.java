package com.asbresearch.collector.event;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

public class TeamNameIgnoreTokenRemover {
    private static final Pattern PATTERN = Pattern.compile("(^[^ ]{2}\\s+)|(\\s+[^ ]{2}$)|" +
            "(^reserve[s]?\\s+)|(\\s+reserve[s]?$)|" +
            "(^\\([^\\)]+\\)\\s+)|(\\s+\\([^\\)]+\\)$)|" +
            "(^city\\s+)|(\\s+city$)|" +
            "(^club[e]?\\s+)|(\\s+club[e]?$)|" +
            "(^united\\s+)|(\\s+united$)|" +
            "(^sporting\\s+)|(\\s+sporting$)|" +
            "(^utd\\s+)|(\\s+utd$)|" +
            "(^u[0-9]{2}\\s+)|(\\s+u[0-9]{2}$)|" +
            "(^stade\\s+)|(\\s+stade$)", CASE_INSENSITIVE);

    public String removeIgnoreToken(String teamName) {
        String result = teamName.toLowerCase().replaceAll("\\.", "");
        int length = result.length();
        while (true) {
            result = PATTERN.matcher(result).replaceAll("");
            if (result.length() == length) {
                return result;
            }
            length = result.length();
        }
    }
}
