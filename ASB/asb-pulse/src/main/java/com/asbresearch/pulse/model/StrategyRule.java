package com.asbresearch.pulse.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.Arrays;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@Slf4j
public final class StrategyRule {
    public static final String SUM_OF_IMPLIED_PROBABILITY = "SIP";
    public static final List<String> FUNCTIONS = Arrays.asList(SUM_OF_IMPLIED_PROBABILITY);
    public static final SpelExpressionParser splParser = new SpelExpressionParser();

    @EqualsAndHashCode.Include
    private final String name;
    private final List<String> vars;
    private final String type;
    private final String expr;

    @JsonCreator(mode = Mode.PROPERTIES)
    public StrategyRule(@JsonProperty("name") String name,
                        @JsonProperty("vars") List<String> vars,
                        @JsonProperty("type") String type,
                        @JsonProperty("expr") String expr) {
        checkNotNull(name, "name must be provided");
        checkNotNull(vars, "vars must be provided");
        checkArgument(!vars.isEmpty());
        checkNotNull(type, "type must be provided");
        checkNotNull(expr, "expr must be provided");
        checkArgument(type.matches("odd|size"), " type value can only be odd or size");
        checkArgument(isValidExpr(vars, expr), String.format("%s cannot be parsed", expr));

        this.name = name;
        this.vars = vars;
        this.expr = expr;
        this.type = type;
    }

    private boolean isValidExpr(List<String> vars, String expr) {
        String expressionText = expr;
        for (String var : vars) {
            expressionText = expressionText.replaceAll(var, "1.0");
        }
        try {
            splParser.parseExpression(expressionText);
            return true;
        } catch (ParseException ex) {
            log.error("Error parsing expr={}", expr, ex);
            return false;
        }
    }

    public static StrategyRule of(String name, List<String> vars, String type, String value) {
        return new StrategyRule(name, vars, type, value);
    }
}
