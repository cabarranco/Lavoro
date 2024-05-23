package com.asbresearch.pulse.mapping;

import com.asbresearch.betfair.ref.entities.MarketCatalogue;
import com.asbresearch.betfair.ref.entities.RunnerCatalog;
import com.asbresearch.betfair.ref.enums.MarketType;
import com.asbresearch.pulse.service.MarketSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BetfairMarketTypeMapping {

    public MarketType marketType(String userMarketType) {
        switch (userMarketType) {
            case "MO":
                return MarketType.MATCH_ODDS;
            case "CS":
                return MarketType.CORRECT_SCORE;
            case "OU05":
                return MarketType.OVER_UNDER_05;
            case "OU15":
                return MarketType.OVER_UNDER_15;
            case "OU25":
                return MarketType.OVER_UNDER_25;
            case "OU35":
                return MarketType.OVER_UNDER_35;
            case "AH":
                return MarketType.ASIAN_HANDICAP;
            default:
                throw new UnsupportedOperationException(String.format("userMarketType=%s", userMarketType));
        }
    }

    public List<MarketSelection> getMarketSelections(List<UserRunnerCode> userRunnerCodes, List<MarketCatalogue> marketCatalogue) {
        List<MarketSelection> result = new ArrayList<>();
        for (UserRunnerCode userRunnerCode : userRunnerCodes) {
            result.addAll(findSelections(marketCatalogue, userRunnerCode));
        }
        return result;
    }

    protected List<MarketSelection> findSelections(List<MarketCatalogue> marketCatalogue, UserRunnerCode userRunnerCode) {
        List<MarketSelection> result = new ArrayList<>();
        switch (userRunnerCode.getMarket()) {
            case "MO":
                result.addAll(getMatchOdds(marketCatalogue, userRunnerCode));
                break;
            case "CS":
                result.addAll(getCorrectScore(marketCatalogue, userRunnerCode));
            case "OU05":
                result.addAll(getOverUnders(marketCatalogue, userRunnerCode, "Over/Under 0.5 Goals"));
                break;
            case "OU15":
                result.addAll(getOverUnders(marketCatalogue, userRunnerCode, "Over/Under 1.5 Goals"));
                break;
            case "OU25":
                result.addAll(getOverUnders(marketCatalogue, userRunnerCode, "Over/Under 2.5 Goals"));
                break;
            case "OU35":
                result.addAll(getOverUnders(marketCatalogue, userRunnerCode, "Over/Under 3.5 Goals"));
                break;
        }
        return result;
    }

    private List<MarketSelection> getOverUnders(List<MarketCatalogue> marketCatalogue, UserRunnerCode userRunnerCode, String marketName) {
        List<MarketSelection> result = new ArrayList<>();
        List<MarketCatalogue> overUnders = marketCatalogue.stream().filter(catalogue -> marketName.equals(catalogue.getMarketName())).collect(Collectors.toList());
        switch (userRunnerCode.getSelection()) {
            case "U":
                result.addAll(selectionByIndex(overUnders, 0, userRunnerCode));
                break;
            case "O":
                result.addAll(selectionByIndex(overUnders, 1, userRunnerCode));
                break;
        }
        return result;
    }

    protected List<MarketSelection> getCorrectScore(List<MarketCatalogue> marketCatalogue, UserRunnerCode userRunnerCode) {
        List<MarketSelection> result = new ArrayList<>();
        List<MarketCatalogue> correctScores = marketCatalogue.stream().filter(catalogue -> "Correct Score".equals(catalogue.getMarketName())).collect(Collectors.toList());
        switch (userRunnerCode.getSelection()) {
            case "00":
                result.addAll(selectionByRunnerName(correctScores, "0 - 0", userRunnerCode));
                break;
            case "01":
                result.addAll(selectionByRunnerName(correctScores, "0 - 1", userRunnerCode));
                break;
            case "02":
                result.addAll(selectionByRunnerName(correctScores, "0 - 2", userRunnerCode));
                break;
            case "03":
                result.addAll(selectionByRunnerName(correctScores, "0 - 3", userRunnerCode));
                break;
            case "10":
                result.addAll(selectionByRunnerName(correctScores, "1 - 0", userRunnerCode));
                break;
            case "11":
                result.addAll(selectionByRunnerName(correctScores, "1 - 1", userRunnerCode));
                break;
            case "12":
                result.addAll(selectionByRunnerName(correctScores, "1 - 2", userRunnerCode));
                break;
            case "13":
                result.addAll(selectionByRunnerName(correctScores, "1 - 3", userRunnerCode));
                break;
            case "20":
                result.addAll(selectionByRunnerName(correctScores, "2 - 0", userRunnerCode));
                break;
            case "21":
                result.addAll(selectionByRunnerName(correctScores, "2 - 1", userRunnerCode));
                break;
            case "22":
                result.addAll(selectionByRunnerName(correctScores, "2 - 2", userRunnerCode));
                break;
            case "23":
                result.addAll(selectionByRunnerName(correctScores, "2 - 3", userRunnerCode));
                break;
            case "30":
                result.addAll(selectionByRunnerName(correctScores, "3 - 0", userRunnerCode));
                break;
            case "31":
                result.addAll(selectionByRunnerName(correctScores, "3 - 1", userRunnerCode));
                break;
            case "32":
                result.addAll(selectionByRunnerName(correctScores, "3 - 2", userRunnerCode));
                break;
            case "33":
                result.addAll(selectionByRunnerName(correctScores, "3 - 3", userRunnerCode));
                break;
            case "AOH":
                result.addAll(selectionByRunnerName(correctScores, "Any Other Home Win", userRunnerCode));
                break;
            case "AOA":
                result.addAll(selectionByRunnerName(correctScores, "Any Other Away Win", userRunnerCode));
                break;
            case "AOD":
                result.addAll(selectionByRunnerName(correctScores, "Any Other Draw", userRunnerCode));
                break;
        }
        return result;
    }

    protected List<MarketSelection> selectionByRunnerName(List<MarketCatalogue> marketCatalogues, String betfairRunnerName, UserRunnerCode userRunnerCode) {
        List<MarketSelection> result = new ArrayList<>();
        marketCatalogues.forEach(catalogue -> {
            Optional<RunnerCatalog> anyRunnerCatalog = catalogue.getRunners().stream().filter(runnerCatalog -> betfairRunnerName.equals(runnerCatalog.getRunnerName())).findAny();
            if (anyRunnerCatalog.isPresent()) {
                result.add(MarketSelection.of(catalogue.getEvent(), catalogue.getMarketId(), anyRunnerCatalog.get(), catalogue.getMarketName(), userRunnerCode));
            }
        });
        return result;
    }

    protected List<MarketSelection> selectionByIndex(List<MarketCatalogue> marketCatalogues, int runnerIndex, UserRunnerCode userRunnerCode) {
        return marketCatalogues.stream().map(catalogue -> MarketSelection.of(catalogue.getEvent(), catalogue.getMarketId(), catalogue.getRunners().get(runnerIndex), catalogue.getMarketName(), userRunnerCode))
                .collect(Collectors.toList());
    }

    protected List<MarketSelection> getMatchOdds(List<MarketCatalogue> marketCatalogue, UserRunnerCode userRunnerCode) {
        List<MarketSelection> result = new ArrayList<>();
        List<MarketCatalogue> matchOdds = marketCatalogue.stream().filter(catalogue -> "Match Odds".equals(catalogue.getMarketName())).collect(Collectors.toList());
        switch (userRunnerCode.getSelection()) {
            case "H":
                result.addAll(selectionByIndex(matchOdds, 0, userRunnerCode));
                break;
            case "A":
                result.addAll(selectionByIndex(matchOdds, 1, userRunnerCode));
                break;
            case "D":
                result.addAll(selectionByIndex(matchOdds, 2, userRunnerCode));
                break;
        }
        return result;
    }
}
