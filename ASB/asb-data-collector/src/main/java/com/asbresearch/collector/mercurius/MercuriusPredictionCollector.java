package com.asbresearch.collector.mercurius;

import com.asbresearch.betfair.ref.BetfairReferenceClient;
import com.asbresearch.betfair.ref.BetfairServerResponse;
import com.asbresearch.betfair.ref.entities.*;
import com.asbresearch.betfair.ref.enums.MarketSort;
import com.asbresearch.betfair.ref.util.Helpers;
import com.asbresearch.collector.betfair.EventsOfTheDayProvider;
import com.asbresearch.common.bigquery.BigQueryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.time.ZoneOffset.UTC;


@AllArgsConstructor
@Component
@Slf4j
@ConditionalOnProperty(prefix = "collector", name = "mercuriusPredictionCollector", havingValue = "on")
public class MercuriusPredictionCollector {
    private static final DecimalFormat decimalFormat = new DecimalFormat("0.00");
    private static final DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm").withZone(UTC);

    private final List<Double> otherHomeWin = new ArrayList<>();
    private final List<Double> otherAwayWin = new ArrayList<>();
    private final List<Double> otherDraw = new ArrayList<>();
    private final Map<Integer, Long> competitionDictionary = new ConcurrentHashMap<>();
    private final Map<Long, Long> teamDictionary = new ConcurrentHashMap<>();

    private final BigQueryService bigQueryService;
    private final EventsOfTheDayProvider eventsOfTheDayProvider;
    private final BetfairReferenceClient betfairReferenceClient;
    private final Cerberus cerberus;

    @PostConstruct
    public void loadStaticTables() {
        log.info("Retrieving mercurius competition ids from asbanalytics.static_tables.competition_dictionary");
        try {
            String query = String.format("SELECT betfairCompetitionId, cerberusCompetitionId FROM `asbanalytics.static_tables.competition_dictionary`");
            List<Map<String, Optional<Object>>> resultSet = bigQueryService.performQuery(query);
            if (!CollectionUtils.isEmpty(resultSet)) {
                resultSet.forEach(row -> {
                    Optional<Object> betfairCompetitionId = row.get("betfairCompetitionId");
                    Optional<Object> cerberusCompetitionId = row.get("cerberusCompetitionId");
                    if (betfairCompetitionId.isPresent() && cerberusCompetitionId.isPresent()) {
                        Integer betfairId = Integer.valueOf((String) betfairCompetitionId.get());
                        Long cerberusId = Long.valueOf((String) cerberusCompetitionId.get());
                        competitionDictionary.put(betfairId, cerberusId);
                    }
                });
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting to retrieve mercurius competition id");
        }
        log.info("Retrieved {} mercurius competition ids from asbanalytics.static_tables.competition_dictionary", competitionDictionary.size());

        log.info("Retrieving mercurius team ids from asbanalytics.static_tables.team_dictionary");
        try {
            String query = String.format("SELECT betfairTeamId, cerberusTeamId FROM `asbanalytics.static_tables.team_dictionary`");
            List<Map<String, Optional<Object>>> resultSet = bigQueryService.performQuery(query);
            if (!CollectionUtils.isEmpty(resultSet)) {
                resultSet.forEach(row -> {
                    Optional<Object> betfairTeamId = row.get("betfairTeamId");
                    Optional<Object> cerberusTeamId = row.get("cerberusTeamId");
                    if (betfairTeamId.isPresent() && cerberusTeamId.isPresent()) {
                        Long betfairId = Long.valueOf((String) betfairTeamId.get());
                        Long cerberusId = Long.valueOf((String) cerberusTeamId.get());
                        teamDictionary.put(betfairId, cerberusId);
                    }
                });
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting to retrieve mercurius team id");
        }
        log.info("Retrieved {} mercurius team ids from asbanalytics.static_tables.team_dictionary", teamDictionary.size());
    }

    @Async
    public void collectMercuriusPredictions() {
        List<MercuriusPrediction> mercuriusPredictions = new ArrayList<>();
        Collection<Event> eventsOfTheDay = eventsOfTheDayProvider.getEventsOfTheDay();
        log.info("Loading mercurius predictions for {} events", eventsOfTheDay.size());
        eventsOfTheDay.forEach(event -> {
            log.info("Loading mercurius odds for eventId={}", event.getId());
            Optional<String> competitionId = getCompetitionFor(event.getId());
            if (competitionId.isPresent()) {
                Map<String, Long> betfairTeamIds = getBetfairTeamIds(event.getId());
                Map<String, Long> cerberusTeamIds = getCerberusTeamId(betfairTeamIds);
                Optional<Long> cerberusCompetitionId = getCerberusCompetitionId(Integer.valueOf(competitionId.get()));
                if (cerberusCompetitionId.isPresent()) {
                    if (cerberusTeamIds.get("home") != null && cerberusTeamIds.get("away") != null) {
                        List<Prediction> predictions = cerberus.getPredictions(cerberusTeamIds.get("home"), cerberusTeamIds.get("away"), df.format(event.getOpenDate()), cerberusCompetitionId.get());
                        if (!predictions.isEmpty()) {
                            mercuriusPredictions.addAll(getFairOdds(predictions, event, Integer.valueOf(event.getId())));
                        }
                    } else {
                        log.error("Cerberus team id={} not found", cerberusCompetitionId.get());
                    }
                } else {
                    log.warn("Missing cerberusCompetitionId for betfair competitionId={}", competitionId.get());
                }
            } else {

            }
        });
        if (!mercuriusPredictions.isEmpty()) {
            List<String> rows = mercuriusPredictions.stream().filter(mercuriusPrediction -> mercuriusPrediction.getEventId() != null && mercuriusPrediction.getRunnerName() != null)
                    .map(MercuriusPrediction::toCsv)
                    .collect(Collectors.toList());
            log.info("Loaded {} rows from mercurius", rows.size());
            bigQueryService.insertRows("betstore", "mercurius_prediction", rows);
        }
    }

    private List<MercuriusPrediction> getFairOdds(List<Prediction> predictions, Event event, Integer eventId) {
        List<MercuriusPrediction> mercuriusPredictions = new ArrayList<>();
        Prediction prediction = predictions.get(0);
        mercuriusPredictions.add(MercuriusPrediction.builder().eventId(eventId).runnerName("Home").backFairPrice(Double.valueOf(decimalFormat.format(1 / (prediction.getHome() / 100)))).build());
        mercuriusPredictions.add(MercuriusPrediction.builder().eventId(eventId).runnerName("Draw").backFairPrice(Double.valueOf(decimalFormat.format(1 / (prediction.getDraw() / 100)))).build());
        mercuriusPredictions.add(MercuriusPrediction.builder().eventId(eventId).runnerName("Away").backFairPrice(Double.valueOf(decimalFormat.format(1 / (prediction.getAway() / 100)))).build());

        prediction.getTotalGoals().forEach((name, odd) -> {
            String fairName = getBackFairPriceName(name);
            Double fairOdd = getBackFairPriceValue(fairName, odd);
            if (fairOdd != null) {
                mercuriusPredictions.add(MercuriusPrediction.builder().eventId(eventId).runnerName(getBackFairPriceName(name)).backFairPrice(getBackFairPriceValue(name, odd)).build());
            }
        });
        if (!otherAwayWin.isEmpty()) {
            mercuriusPredictions.add(MercuriusPrediction.builder().eventId(eventId).runnerName("Any Other Away Win").backFairPrice(Double.valueOf(decimalFormat.format(1 / sum(otherAwayWin)))).build());
        }
        if (!otherHomeWin.isEmpty()) {
            mercuriusPredictions.add(MercuriusPrediction.builder().eventId(eventId).runnerName("Any Other home Win").backFairPrice(Double.valueOf(decimalFormat.format(1 / sum(otherHomeWin)))).build());
        }
        if (!otherDraw.isEmpty()) {
            mercuriusPredictions.add(MercuriusPrediction.builder().eventId(eventId).runnerName("Any Other Draw").backFairPrice(Double.valueOf(decimalFormat.format(1 / sum(otherDraw)))).build());
        }
        return mercuriusPredictions;
    }

    private Double getBackFairPriceValue(String fairName, Double odd) {
        if ("Any Other Draw".equalsIgnoreCase(fairName)) {
            otherDraw.add(odd);
            return null;
        }
        if ("Any Other Home Win".equalsIgnoreCase(fairName)) {
            otherHomeWin.add(odd);
            return null;
        }
        if ("Any Other Away Win".equalsIgnoreCase(fairName)) {
            otherAwayWin.add(odd);
            return null;
        }
        return Double.valueOf(decimalFormat.format(1 / odd));
    }

    private String getBackFairPriceName(String runnerName) {
        log.info("Retrieving back fair price name...");
        Integer home = Integer.valueOf(runnerName.split("-")[0]);
        Integer away = Integer.valueOf(runnerName.split("-")[1]);
        if (home <= 3 && away <= 3) return runnerName;
        if (home > away) return "Any Other Home Win";
        if (away > home) return "Any Other Away Win";
        if (away.equals(home)) return "Any Other Draw";
        return runnerName;
    }

    private Optional<String> getCompetitionFor(String eventId) {
        BetfairServerResponse<List<CompetitionResult>> competitionResponse = betfairReferenceClient.listCompetitions(getMarketFilter(eventId));
        if (competitionResponse != null) {
            List<String> result = competitionResponse.getResponse().stream().map(competitionResult -> competitionResult.getCompetition().getId()).collect(Collectors.toList());
            if (!result.isEmpty() && result.size() == 1) {
                return Optional.of(result.iterator().next());
            }
        }
        return Optional.empty();
    }

    private MarketFilter getMarketFilter(String eventId) {
        MarketFilter marketFilter = new MarketFilter();
        marketFilter.setEventIds(Collections.singleton(eventId));
        return marketFilter;
    }

    private Map<String, Long> getCerberusTeamId(Map<String, Long> betfairIds) {
        Map<String, Long> result = new HashMap<>();
        Long home = betfairIds.get("home");
        Long away = betfairIds.get("away");
        if (home != null && away != null) {
            Long cerberusHome = teamDictionary.get(home);
            Long cerberusAway = teamDictionary.get(away);
            if (cerberusHome != null && cerberusAway != null) {
                result.put("home", cerberusHome);
                result.put("away", cerberusAway);
            }
            log.info("Home: " + result.get("home"));
            log.info("Away: " + result.get("away"));
        }
        return result;
    }

    private Map<String, Long> getBetfairTeamIds(String eventId) {
        log.info("Retrieving betfair team ids for event={}", eventId);
        Map<String, Long> result = new HashMap<>();
        BetfairServerResponse<List<MarketCatalogue>> marketCatalogueResponse = betfairReferenceClient.listMarketCatalogue(getMarketFilter(eventId), Helpers.soccerMatchProjection(), MarketSort.FIRST_TO_START, 400);
        if (marketCatalogueResponse != null) {
            List<MarketCatalogue> catalogues = marketCatalogueResponse.getResponse();
            if (!catalogues.isEmpty()) {
                MarketCatalogue firstCatalogue = catalogues.iterator().next();
                if (!CollectionUtils.isEmpty(firstCatalogue.getRunners()) && firstCatalogue.getRunners().size() > 1) {
                    List<RunnerCatalog> runners = firstCatalogue.getRunners();
                    result.put("home", runners.get(0).getSelectionId());
                    result.put("away", runners.get(1).getSelectionId());
                    String homeName = runners.get(0).getRunnerName();
                    String awayName = runners.get(1).getRunnerName();
                    log.info("Home:{} {}", homeName, result.get("home"));
                    log.info("Away:{} {}", awayName, result.get("away"));
                }
            }
        }
        return result;
    }

    private Optional<Long> getCerberusCompetitionId(Integer betfairCompetitionId) {
        Long result = competitionDictionary.get(betfairCompetitionId);
        return result == null ? Optional.empty() : Optional.of(result);
    }

    private double sum(List<Double> values) {
        return values.stream().filter(Objects::nonNull).mapToDouble(value -> value).sum();
    }
}
