package com.asbresearch.betfair.ref;

import com.asbresearch.betfair.ref.config.AppConfig;
import com.asbresearch.betfair.ref.entities.AccountDetailsResponse;
import com.asbresearch.betfair.ref.entities.AccountFundsResponse;
import com.asbresearch.betfair.ref.entities.AccountStatementReport;
import com.asbresearch.betfair.ref.entities.EventResult;
import com.asbresearch.betfair.ref.entities.LimitOrder;
import com.asbresearch.betfair.ref.entities.MarketBook;
import com.asbresearch.betfair.ref.entities.MarketCatalogue;
import com.asbresearch.betfair.ref.entities.MarketFilter;
import com.asbresearch.betfair.ref.entities.PlaceExecutionReport;
import com.asbresearch.betfair.ref.entities.PlaceInstruction;
import com.asbresearch.betfair.ref.entities.RunnerCatalog;
import com.asbresearch.betfair.ref.entities.StatementItem;
import com.asbresearch.betfair.ref.entities.TimeRange;
import com.asbresearch.betfair.ref.enums.Exchange;
import com.asbresearch.betfair.ref.enums.ExecutionReportStatus;
import com.asbresearch.betfair.ref.enums.IncludeItem;
import com.asbresearch.betfair.ref.enums.MarketSort;
import com.asbresearch.betfair.ref.enums.MarketType;
import com.asbresearch.betfair.ref.enums.MatchProjection;
import com.asbresearch.betfair.ref.enums.OrderProjection;
import com.asbresearch.betfair.ref.enums.OrderType;
import com.asbresearch.betfair.ref.enums.PersistenceType;
import com.asbresearch.betfair.ref.enums.Side;
import com.asbresearch.betfair.ref.enums.Wallet;
import com.asbresearch.betfair.ref.exceptions.LoginException;
import com.asbresearch.betfair.ref.util.Helpers;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static java.util.Collections.singleton;

@Slf4j
public class BetfairReferenceClientTest {
    AppConfig appConfig = new AppConfig("ZBsLSgTAiftsAy2R", "fdr@asbresearch.com", "asbcheqai87");
//    AppConfig appConfig = new AppConfig("Wowip95D4KxDk2tr", "Mikelevirgo2", "cheqai87");

    private BetfairReferenceClient client;
    private Set<String> marketTypeCodes;

    @Before
    public void setUp() throws LoginException {
        client = new BetfairReferenceClient(Exchange.UK, appConfig.getAppKey());
        client.login(appConfig.getUserName(), appConfig.getPassword());

        marketTypeCodes = new HashSet<>();
        marketTypeCodes.add(MarketType.MATCH_BET.toString());
        marketTypeCodes.add(MarketType.MATCH_ODDS_AND_BTTS.toString());
        marketTypeCodes.add(MarketType.MATCH_ODDS.toString());
    }

    @Test
    public void getAccountStatement() {
        Instant now = Instant.now();
        BetfairServerResponse<AccountStatementReport> response = client.getAccountStatement(0, 100, new TimeRange(now.minus(5, ChronoUnit.DAYS), now), IncludeItem.ALL, Wallet.UK);
        if (response != null) {
            AccountStatementReport accountStatementReport = response.getResponse();
            List<StatementItem> accountStatement = accountStatementReport.getAccountStatement();
            accountStatement.forEach(statementItem -> log.info("{}", statementItem));
        }
    }

    @Test
    public void getAccountDetails() {
        BetfairServerResponse<AccountDetailsResponse> response = client.getAccountDetails();
        if (response != null) {
            log.info("response={}", response.getResponse());
        }
    }

    @Test
//    @Ignore
    public void getAccountFunds() {
        BetfairServerResponse<AccountFundsResponse> response = client.getAccountFunds(Wallet.UK);
        if (response != null) {
            AccountFundsResponse accountFundsResponse = response.getResponse();
            log.info("accountFundsResponse={}", accountFundsResponse);
        }
    }

    @Test
    @Ignore
    public void allTeams() {
        MarketFilter filter = new MarketFilter();
        filter.setEventTypeIds(singleton("1"));
        Instant now = Instant.now();
        now.minus(30 * 6, ChronoUnit.DAYS);
        filter.setMarketStartTime(new TimeRange(now.minus(30 * 6, ChronoUnit.DAYS), now.plus(30 * 6, ChronoUnit.DAYS)));
        BetfairServerResponse<List<EventResult>> response = client.listEvents(filter);
        Map<String, String> teams = new HashMap<>();
        if (response != null) {
            List<EventResult> events = response.getResponse();
            if (events != null) {
                events.forEach(eventResult -> addTeams(teams, eventResult));
            }
        }
        for (Map.Entry<String, String> entry : teams.entrySet()) {
            log.info("{},{}", entry.getValue(), entry.getKey());
        }
    }

    @Test
    public void liveEvents() {
        MarketFilter filter = new MarketFilter();
        filter.setEventTypeIds(singleton("1"));
        filter.setInPlayOnly(true);
        BetfairServerResponse<List<EventResult>> response = client.listEvents(filter);

        if (response != null) {
            List<EventResult> events = response.getResponse();
            if (events != null) {
                log.info("Currently {} live events", events.size());
                events.forEach(eventResult -> {
                    log.info("name={} countryCode={} startTime={}",
                            eventResult.getEvent().getName(),
                            eventResult.getEvent().getCountryCode(),
                            eventResult.getEvent().getOpenDate());
                });
            }
        }
    }

    private void addTeams(Map<String, String> teams, EventResult eventResult) {
        String countryCode = eventResult.getEvent().getCountryCode();
        String name = eventResult.getEvent().getName();
        String[] teamNames = name.split(" v ");
        try {
            teams.putIfAbsent(teamNames[0], countryCode);
            teams.putIfAbsent(teamNames[1], countryCode);
        } catch (ArrayIndexOutOfBoundsException ex) {
            log.error("Error splitting event={}", name);
        }
    }


    @Test
    public void listMarketCatalogue() {
        Instant now = Instant.now();
        TimeRange localTimeRange = new TimeRange(now, now.plus(90, ChronoUnit.DAYS));

        BetfairServerResponse<List<MarketCatalogue>> serverResponse = client.listMarketCatalogue(
                Helpers.soccerMatchFilter(null, localTimeRange, marketTypeCodes, "FCSB"),
                Helpers.soccerMatchProjection(),
                MarketSort.FIRST_TO_START, 40);
        List<MarketCatalogue> marketCatalogueList = serverResponse.getResponse();

        for (MarketCatalogue marketCatalogue : marketCatalogueList) {
            log.info("Market Name={} Id={} Competition={} Time={}",
                    marketCatalogue.getMarketName(),
                    marketCatalogue.getMarketId(),
                    marketCatalogue.getCompetition(),
                    marketCatalogue.getDescription().getMarketTime());

            List<RunnerCatalog> runners = marketCatalogue.getRunners();
            if (runners != null) {
                for (RunnerCatalog rCat : runners) {
                    log.info("Runner Name={} SelectionId={}", rCat.getRunnerName(), rCat.getSelectionId());
                }
            }

        }
    }

    @Test
    @Ignore
    public void placeBets() {
        List<MarketCatalogue> marketCatalogueList = client.listMarketCatalogue(
                Helpers.soccerMatchFilter(null, null, marketTypeCodes, null),
                Helpers.soccerMatchProjection(),
                MarketSort.FIRST_TO_START, 5).getResponse();

        for (MarketCatalogue marketCatalogue : marketCatalogueList) {
            // this call is only to see which are available bets for an event
            List<MarketBook> marketBookList = client.listMarketBook(Collections.singletonList(marketCatalogue.getMarketId()), Helpers.soccerPriceProjection(),
                    OrderProjection.ALL, MatchProjection.NO_ROLLUP).getResponse();
            for (MarketBook marketBook : marketBookList) {
                log.info("{}", marketBook.getRunners());
                Long selectionId = marketBook.getRunners().get(0).getSelectionId();
                LimitOrder limitOrder = new LimitOrder(2, Helpers.getMaxBetIncrement(marketBook.getRunners().get(0).getLastPriceTraded()), PersistenceType.LAPSE);
                PlaceInstruction placeInstruction = new PlaceInstruction(OrderType.LIMIT, selectionId, 0, Side.LAY, limitOrder, null, null);
                PlaceExecutionReport placeExecutionReport = client.placeOrders(marketCatalogue.getMarketId(), Collections.singletonList(placeInstruction), null, marketBook.getVersion()).getResponse();
                if (placeExecutionReport != null) {
                    if (ExecutionReportStatus.FAILURE.equals(placeExecutionReport.getStatus())) {
                        log.info("{} - {}", placeExecutionReport.getErrorCode(), placeExecutionReport.getInstructionReports().get(0).getErrorCode());
                    } else {
                        log.info("{} - {}", placeExecutionReport.getStatus(), placeExecutionReport.getInstructionReports().get(0).getStatus());
                    }
                }
            }
        }
    }

    @Test
    @Ignore
    public void placeSingleBets() {
        Long selectionId = 1222344L;
        LimitOrder limitOrder = new LimitOrder(2, 1.01, PersistenceType.LAPSE);
        PlaceInstruction placeInstruction = new PlaceInstruction(OrderType.LIMIT, selectionId, 0, Side.BACK, limitOrder, null, null);
        PlaceExecutionReport placeExecutionReport = client.placeOrders("1.170466619", Collections.singletonList(placeInstruction), null, null).getResponse();
        if (placeExecutionReport != null) {
            if (ExecutionReportStatus.FAILURE.equals(placeExecutionReport.getStatus())) {
                log.info("{} - {}", placeExecutionReport.getErrorCode(), placeExecutionReport.getInstructionReports().get(0).getErrorCode());
            } else {
                log.info("{} - {}", placeExecutionReport.getStatus(), placeExecutionReport.getInstructionReports().get(0).getStatus());
            }
        }
    }
}