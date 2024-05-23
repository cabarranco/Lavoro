package com.asbresearch.betfair.ref;


import com.asbresearch.betfair.ref.entities.AccountDetailsResponse;
import com.asbresearch.betfair.ref.entities.AccountFundsResponse;
import com.asbresearch.betfair.ref.entities.AccountStatementReport;
import com.asbresearch.betfair.ref.entities.CancelExecutionReport;
import com.asbresearch.betfair.ref.entities.CancelInstruction;
import com.asbresearch.betfair.ref.entities.ClearedOrderSummaryReport;
import com.asbresearch.betfair.ref.entities.CompetitionResult;
import com.asbresearch.betfair.ref.entities.CountryCodeResult;
import com.asbresearch.betfair.ref.entities.CurrencyRate;
import com.asbresearch.betfair.ref.entities.CurrentOrderSummaryReport;
import com.asbresearch.betfair.ref.entities.EventResult;
import com.asbresearch.betfair.ref.entities.EventTypeResult;
import com.asbresearch.betfair.ref.entities.MarketBook;
import com.asbresearch.betfair.ref.entities.MarketCatalogue;
import com.asbresearch.betfair.ref.entities.MarketFilter;
import com.asbresearch.betfair.ref.entities.MarketProfitAndLoss;
import com.asbresearch.betfair.ref.entities.MarketTypeResult;
import com.asbresearch.betfair.ref.entities.MarketVersion;
import com.asbresearch.betfair.ref.entities.PlaceExecutionReport;
import com.asbresearch.betfair.ref.entities.PlaceInstruction;
import com.asbresearch.betfair.ref.entities.PriceProjection;
import com.asbresearch.betfair.ref.entities.ReplaceExecutionReport;
import com.asbresearch.betfair.ref.entities.ReplaceInstruction;
import com.asbresearch.betfair.ref.entities.TimeRange;
import com.asbresearch.betfair.ref.entities.TimeRangeResult;
import com.asbresearch.betfair.ref.entities.TransferResponse;
import com.asbresearch.betfair.ref.entities.UpdateExecutionReport;
import com.asbresearch.betfair.ref.entities.UpdateInstruction;
import com.asbresearch.betfair.ref.entities.VenueResult;
import com.asbresearch.betfair.ref.enums.BetStatus;
import com.asbresearch.betfair.ref.enums.Endpoint;
import com.asbresearch.betfair.ref.enums.Exchange;
import com.asbresearch.betfair.ref.enums.GroupBy;
import com.asbresearch.betfair.ref.enums.IncludeItem;
import com.asbresearch.betfair.ref.enums.MarketProjection;
import com.asbresearch.betfair.ref.enums.MarketSort;
import com.asbresearch.betfair.ref.enums.MatchProjection;
import com.asbresearch.betfair.ref.enums.OrderBy;
import com.asbresearch.betfair.ref.enums.OrderProjection;
import com.asbresearch.betfair.ref.enums.Side;
import com.asbresearch.betfair.ref.enums.SortDir;
import com.asbresearch.betfair.ref.enums.TimeGranularity;
import com.asbresearch.betfair.ref.enums.Wallet;
import com.asbresearch.betfair.ref.exceptions.LoginException;
import com.asbresearch.betfair.ref.util.Constants;
import com.asbresearch.betfair.ref.util.Helpers;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.HttpsURLConnection;
import lombok.extern.slf4j.Slf4j;

import static com.asbresearch.betfair.ref.enums.Endpoint.Betting;
import static java.util.Collections.singletonMap;

@Slf4j
public class BetfairReferenceClient {

    private final Exchange exchange;
    private final String appKey;
    private String sessionToken;
    private Network networkClient;
    private final ObjectMapper mapper = new ObjectMapper();

    private static String LIST_COMPETITIONS_METHOD = "SportsAPING/v1.0/listCompetitions";
    private static String LIST_COUNTRIES_METHOD = "SportsAPING/v1.0/listCountries";
    private static String LIST_CURRENT_ORDERS_METHOD = "SportsAPING/v1.0/listCurrentOrders";
    private static String LIST_CLEARED_ORDERS_METHOD = "SportsAPING/v1.0/listClearedOrders";
    private static String LIST_EVENT_TYPES_METHOD = "SportsAPING/v1.0/listEventTypes";
    private static String LIST_EVENTS_METHOD = "SportsAPING/v1.0/listEvents";
    private static String LIST_MARKET_CATALOGUE_METHOD = "SportsAPING/v1.0/listMarketCatalogue";
    private static String LIST_MARKET_BOOK_METHOD = "SportsAPING/v1.0/listMarketBook";
    private static String LIST_MARKET_PROFIT_AND_LOSS = "SportsAPING/v1.0/listMarketProfitAndLoss";
    private static String LIST_MARKET_TYPES = "SportsAPING/v1.0/listMarketTypes";
    private static String LIST_TIME_RANGES = "SportsAPING/v1.0/listTimeRanges";
    private static String LIST_VENUES = "SportsAPING/v1.0/listVenues";
    private static String PLACE_ORDERS_METHOD = "SportsAPING/v1.0/placeOrders";
    private static String CANCEL_ORDERS_METHOD = "SportsAPING/v1.0/cancelOrders";
    private static String REPLACE_ORDERS_METHOD = "SportsAPING/v1.0/replaceOrders";
    private static String UPDATE_ORDERS_METHOD = "SportsAPING/v1.0/updateOrders";

    private static String GET_ACCOUNT_DETAILS = "AccountAPING/v1.0/getAccountDetails";
    private static String GET_ACCOUNT_FUNDS = "AccountAPING/v1.0/getAccountFunds";
    private static String GET_ACCOUNT_STATEMENT = "AccountAPING/v1.0/getAccountStatement";
    private static String LIST_CURRENCY_RATES = "AccountAPING/v1.0/listCurrencyRates";
    private static String TRANSFER_FUNDS = "AccountAPING/v1.0/transferFunds";

    private static String FILTER = "filter";
    private static String BET_IDS = "betIds";
    private static String RUNNER_IDS = "runnerIds";
    private static String SIDE = "side";
    private static String SETTLED_DATE_RANGE = "settledDateRange";
    private static String EVENT_TYPE_IDS = "eventTypeIds";
    private static String EVENT_IDS = "eventIds";
    private static String BET_STATUS = "betStatus";
    private static String PLACED_DATE_RANGE = "placedDateRange";
    private static String DATE_RANGE = "dateRange";
    private static String ORDER_BY = "orderBy";
    private static String GROUP_BY = "groupBy";
    private static String SORT_DIR = "sortDir";
    private static String FROM_RECORD = "fromRecord";
    private static String RECORD_COUNT = "recordCount";
    private static String GRANULARITY = "granularity";
    private static String MARKET_PROJECTION = "marketProjection";
    private static String MATCH_PROJECTION = "matchProjection";
    private static String ORDER_PROJECTION = "orderProjection";
    private static String PRICE_PROJECTION = "priceProjection";
    private static String SORT = "sort";
    private static String MAX_RESULTS = "maxResults";
    private static String MARKET_IDS = "marketIds";
    private static String MARKET_ID = "marketId";
    private static String INSTRUCTIONS = "instructions";
    private static String CUSTOMER_REFERENCE = "customerRef";
    private static String MARKET_VERSION = "marketVersion";
    private static String INCLUDE_SETTLED_BETS = "includeSettledBets";
    private static String INCLUDE_BSP_BETS = "includeBspBets";
    private static String INCLUDE_ITEM_DESCRIPTION = "includeItemDescription";
    private static String NET_OF_COMMISSION = "netOfCommission";
    private static String FROM_CURRENCY = "fromCurrency";
    private static String FROM = "from";
    private static String TO = "to";
    private static String AMOUNT = "amount";
    private static String WALLET = "wallet";
    private static String ITEM_DATE_RANGE = "itemDateRange";
    private static String INCLUDE_ITEM = "includeItem";


    /**
     * Static defined identity endpoints
     */
    private static HashMap<Exchange, String> identityEndpoints = new HashMap<>();

    static {
        identityEndpoints.put(Exchange.RO, "https://identitysso.betfair.ro/api/login");
        identityEndpoints.put(Exchange.UK, "https://identitysso.betfair.com/api/login");
        identityEndpoints.put(Exchange.AUS, "https://identitysso.betfair.com/api/login");
        identityEndpoints.put(Exchange.IT, "https://identitysso.betfair.it/api/login");
        identityEndpoints.put(Exchange.ES, "https://identitysso.betfair.es/api/login");
    }

    public BetfairReferenceClient(Exchange exchange, String appKey) {
        this.exchange = exchange;
        this.appKey = appKey;
    }

    public Boolean login(String username, String password) throws LoginException {
        if (Helpers.isNullOrWhitespace(username))
            throw new IllegalArgumentException(username);
        if (Helpers.isNullOrWhitespace(password))
            throw new IllegalArgumentException(password);
        try {
            URL url = new URL(identityEndpoints.get(exchange));
            String postData = String.format("username=%s&password=%s", username, password);

            HttpsURLConnection request = (HttpsURLConnection) url.openConnection();
            request.setRequestMethod("POST");
            request.setRequestProperty("X-Application", this.appKey);
            request.setRequestProperty("Accept", "application/json");
            request.setDoOutput(true);
            DataOutputStream writer = new DataOutputStream(request.getOutputStream());
            writer.writeBytes(postData);
            writer.flush();
            writer.close();

            BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            if (log.isDebugEnabled()) {
                log.debug(response.toString());
            }

            LoginResponse loginResult = mapper.readValue(response.toString(), LoginResponse.class);
            if (loginResult.getStatus().equals(Constants.SUCCESS)) {
                this.sessionToken = loginResult.getToken();
                log.info("login={} sessionToken={}", username, this.sessionToken);
                this.networkClient = new Network(this.appKey, this.sessionToken, mapper);
                return true;
            } else {
                return false;
            }
        } catch (Exception ex) {
            throw new LoginException(ex);
        }
    }

    public BetfairServerResponse<KeepAliveResponse> keepAlive() {
        return networkClient.keepAliveSynchronous();
    }

    public BetfairServerResponse<List<CompetitionResult>> listCompetitions(MarketFilter marketFilter) {
        JavaType resultType = mapper.getTypeFactory().constructCollectionType(List.class, CompetitionResult.class);
        return networkClient.invoke(resultType, this.exchange, Betting, LIST_COMPETITIONS_METHOD, marketFilterMapping(marketFilter));
    }

    private Map<String, Object> marketFilterMapping(MarketFilter marketFilter) {
        return singletonMap(FILTER, marketFilter);
    }

    public BetfairServerResponse<List<CountryCodeResult>> listCountries(MarketFilter marketFilter) {
        JavaType resultType = mapper.getTypeFactory().constructCollectionType(List.class, CountryCodeResult.class);
        return networkClient.invoke(resultType, exchange, Betting, LIST_COUNTRIES_METHOD, marketFilterMapping(marketFilter));
    }

    public BetfairServerResponse<List<EventResult>> listEvents(MarketFilter marketFilter) {
        JavaType resultType = mapper.getTypeFactory().constructCollectionType(List.class, EventResult.class);
        return networkClient.invoke(resultType, exchange, Betting, LIST_EVENTS_METHOD, marketFilterMapping(marketFilter));
    }

    public BetfairServerResponse<List<EventTypeResult>> listEventTypes(MarketFilter marketFilter) {
        JavaType resultType = mapper.getTypeFactory().constructCollectionType(List.class, EventTypeResult.class);
        return networkClient.invoke(resultType, exchange, Betting, LIST_EVENT_TYPES_METHOD, marketFilterMapping(marketFilter));
    }

    public BetfairServerResponse<List<MarketBook>> listMarketBook(
            List<String> marketIds,
            PriceProjection priceProjection,
            OrderProjection orderProjection,
            MatchProjection matchProjection) {
        Map<String, Object> args = ImmutableMap.of(MARKET_IDS, marketIds,
                PRICE_PROJECTION, priceProjection,
                ORDER_PROJECTION, orderProjection,
                MATCH_PROJECTION, matchProjection);
        JavaType resultType = mapper.getTypeFactory().constructCollectionType(List.class, MarketBook.class);
        return networkClient.invoke(resultType, exchange, Betting, LIST_MARKET_BOOK_METHOD, args);
    }

    public BetfairServerResponse<List<MarketCatalogue>> listMarketCatalogue(
            MarketFilter marketFilter,
            Set<MarketProjection> marketProjections,
            MarketSort sort,
            int maxResult) {
        Map<String, Object> args = ImmutableMap.of(FILTER, marketFilter,
                MARKET_PROJECTION, marketProjections,
                SORT, sort,
                MAX_RESULTS, maxResult);
        JavaType resultType = mapper.getTypeFactory().constructCollectionType(List.class, MarketCatalogue.class);
        return networkClient.invoke(resultType, exchange, Betting, LIST_MARKET_CATALOGUE_METHOD, args);
    }

    public BetfairServerResponse<List<MarketTypeResult>> listMarketTypes(MarketFilter marketFilter) {
        JavaType resultType = mapper.getTypeFactory().constructCollectionType(List.class, MarketTypeResult.class);
        return networkClient.invoke(resultType,
                this.exchange,
                Betting,
                LIST_MARKET_TYPES,
                marketFilterMapping(marketFilter));
    }

    public BetfairServerResponse<List<MarketProfitAndLoss>> listMarketProfitAndLoss(
            List<String> marketIds,
            Boolean includeSettledBets,
            Boolean includeBsbBets,
            Boolean netOfComission) {
        Map<String, Object> args = ImmutableMap.of(MARKET_IDS, marketIds,
                INCLUDE_SETTLED_BETS, includeSettledBets,
                INCLUDE_BSP_BETS, includeBsbBets,
                NET_OF_COMMISSION, netOfComission);
        JavaType resultType = mapper.getTypeFactory().constructCollectionType(List.class, MarketProfitAndLoss.class);
        return networkClient.invoke(resultType,
                this.exchange,
                Betting,
                LIST_MARKET_PROFIT_AND_LOSS,
                args);
    }

    public BetfairServerResponse<List<TimeRangeResult>> listTimeRanges(
            MarketFilter marketFilter,
            TimeGranularity timeGranularity) {
        Map<String, Object> args = ImmutableMap.of(FILTER, marketFilter, GRANULARITY, timeGranularity);
        JavaType resultType = mapper.getTypeFactory().constructCollectionType(List.class, TimeRangeResult.class);
        return networkClient.invoke(resultType, exchange, Betting, LIST_TIME_RANGES, args);
    }

    public BetfairServerResponse<List<VenueResult>> listVenues(MarketFilter marketFilter) {
        JavaType resultType = mapper.getTypeFactory().constructCollectionType(List.class, VenueResult.class);
        return networkClient.invoke(
                resultType,
                exchange,
                Betting,
                LIST_VENUES,
                marketFilterMapping(marketFilter));
    }

    public BetfairServerResponse<CurrentOrderSummaryReport> listCurrentOrders(
            Set<String> betIds,
            Set<String> marketIds,
            OrderProjection orderProjection,
            TimeRange placedDateRange,
            TimeRange dateRange,
            OrderBy orderBy,
            SortDir sortDir,
            Optional<Integer> fromRecord,
            Optional<Integer> recordCount) {
        Map<String, Object> args = new HashMap<>();
        args.put(BET_IDS, betIds);
        args.put(MARKET_IDS, marketIds);
        args.put(ORDER_PROJECTION, orderProjection);
        args.put(PLACED_DATE_RANGE, placedDateRange);
        args.put(DATE_RANGE, dateRange);
        args.put(ORDER_BY, orderBy);
        args.put(SORT_DIR, sortDir);
        if (fromRecord != null && fromRecord.isPresent()) {
            args.put(FROM_RECORD, fromRecord.get());
        }
        if (recordCount != null && recordCount.isPresent()) {
            args.put(RECORD_COUNT, recordCount.get());
        }
        JavaType resultType = mapper.getTypeFactory().constructType(CurrentOrderSummaryReport.class);
        return networkClient.invoke(resultType,
                exchange,
                Betting,
                LIST_CURRENT_ORDERS_METHOD,
                args);
    }

    public BetfairServerResponse<ClearedOrderSummaryReport> listClearedOrders(
            BetStatus betStatus,
            Set<String> eventTypeIds,
            Set<String> eventIds,
            Set<String> marketIds,
            Set<String> betIds,
            Side side,
            TimeRange settledDateRange,
            GroupBy groupBy,
            Boolean includeItemDescription,
            Integer fromRecord,
            Integer recordCount) {
        Map<String, Object> args = new HashMap<>();
        args.put(BET_STATUS, betStatus);
        args.put(EVENT_TYPE_IDS, eventTypeIds);
        args.put(EVENT_IDS, eventIds);
        args.put(MARKET_IDS, marketIds);
        args.put(BET_IDS, betIds);
        args.put(SIDE, side);
        args.put(DATE_RANGE, settledDateRange);
        args.put(GROUP_BY, groupBy);
        args.put(INCLUDE_ITEM_DESCRIPTION, includeItemDescription);
        args.put(FROM_RECORD, fromRecord);
        args.put(RECORD_COUNT, recordCount);

        JavaType resultType = mapper.getTypeFactory().constructType(CurrentOrderSummaryReport.class);
        return networkClient.invoke(resultType, exchange, Betting, LIST_CLEARED_ORDERS_METHOD, args);
    }

    public BetfairServerResponse<PlaceExecutionReport> placeOrders(
            String marketId,
            List<PlaceInstruction> placeInstructions,
            String customerRef) {
        return placeOrders(marketId, placeInstructions, customerRef, null);
    }

    public BetfairServerResponse<PlaceExecutionReport> placeOrders(
            String marketId,
            List<PlaceInstruction> placeInstructions,
            String customerRef,
            Long marketVersion) {
        HashMap<String, Object> args = new HashMap<>();
        args.put(MARKET_ID, marketId);
        args.put(INSTRUCTIONS, placeInstructions);
        args.put(CUSTOMER_REFERENCE, customerRef);
        if (marketVersion != null) {
            args.put(MARKET_VERSION, new MarketVersion(marketVersion));
        }
        JavaType resultType = mapper.getTypeFactory().constructType(PlaceExecutionReport.class);
        return networkClient.invoke(resultType, exchange, Betting, PLACE_ORDERS_METHOD, args);
    }

    public BetfairServerResponse<CancelExecutionReport> cancelOrders(
            String marketId,
            List<CancelInstruction> instructions,
            String customerRef) {
        HashMap<String, Object> args = new HashMap<>();
        args.put(MARKET_ID, marketId);
        args.put(INSTRUCTIONS, instructions);
        args.put(CUSTOMER_REFERENCE, customerRef);

        JavaType resultType = mapper.getTypeFactory().constructType(CancelExecutionReport.class);
        return networkClient.invoke(resultType,
                exchange,
                Betting,
                CANCEL_ORDERS_METHOD,
                args);
    }

    public BetfairServerResponse<ReplaceExecutionReport> replaceOrders(
            String marketId,
            List<ReplaceInstruction> instructions,
            String customerRef) {
        HashMap<String, Object> args = new HashMap<>();
        args.put(MARKET_ID, marketId);
        args.put(INSTRUCTIONS, instructions);
        args.put(CUSTOMER_REFERENCE, customerRef);

        JavaType resultType = mapper.getTypeFactory().constructType(ReplaceExecutionReport.class);
        return networkClient.invoke(
                resultType,
                this.exchange,
                Betting,
                REPLACE_ORDERS_METHOD,
                args);
    }

    public BetfairServerResponse<UpdateExecutionReport> updateOrders(
            String marketId,
            List<UpdateInstruction> instructions,
            String customerRef) {
        HashMap<String, Object> args = new HashMap<>();
        args.put(MARKET_ID, marketId);
        args.put(INSTRUCTIONS, instructions);
        args.put(CUSTOMER_REFERENCE, customerRef);

        JavaType resultType = mapper.getTypeFactory().constructType(UpdateExecutionReport.class);
        return networkClient.invoke(resultType, exchange, Betting, UPDATE_ORDERS_METHOD, args);
    }

    // Account API's
    public BetfairServerResponse<AccountDetailsResponse> getAccountDetails() {
        Map<String, Object> args = new HashMap<>();
        JavaType resultType = mapper.getTypeFactory().constructType(AccountDetailsResponse.class);
        return networkClient.invoke(resultType, exchange, Endpoint.Account, GET_ACCOUNT_DETAILS, args);
    }

    public BetfairServerResponse<AccountFundsResponse> getAccountFunds(Wallet wallet) {
        Map<String, Object> args = new HashMap<>();
        args.put(WALLET, wallet);
        JavaType resultType = mapper.getTypeFactory().constructType(AccountFundsResponse.class);
        return networkClient.invoke(
                resultType,
                this.exchange,
                Endpoint.Account,
                GET_ACCOUNT_FUNDS,
                args);
    }

    public BetfairServerResponse<AccountStatementReport> getAccountStatement(
            Integer fromRecord,
            Integer recordCount,
            TimeRange itemDateRange,
            IncludeItem includeItem,
            Wallet wallet) {
        HashMap<String, Object> args = new HashMap<>();
        args.put(FROM_RECORD, fromRecord);
        args.put(RECORD_COUNT, recordCount);
        args.put(ITEM_DATE_RANGE, itemDateRange);
        args.put(INCLUDE_ITEM, includeItem);
        args.put(WALLET, wallet);
        JavaType resultType = mapper.getTypeFactory().constructType(AccountStatementReport.class);
        return networkClient.invoke(
                resultType,
                this.exchange,
                Endpoint.Account,
                GET_ACCOUNT_STATEMENT,
                args);
    }

    public BetfairServerResponse<List<CurrencyRate>> listCurrencyRates(String fromCurrency) {
        HashMap<String, Object> args = new HashMap<>();
        args.put(FROM_CURRENCY, fromCurrency);
        JavaType resultType = mapper.getTypeFactory().constructCollectionType(List.class, CurrencyRate.class);
        return networkClient.invoke(
                resultType,
                this.exchange,
                Endpoint.Account,
                LIST_CURRENCY_RATES,
                args);
    }

    public BetfairServerResponse<TransferResponse> transferFunds(Wallet from, Wallet to, double amount) {
        HashMap<String, Object> args = new HashMap<>();
        args.put(FROM, from);
        args.put(TO, to);
        args.put(AMOUNT, amount);
        JavaType resultType = mapper.getTypeFactory().constructType(TransferResponse.class);
        return networkClient.invoke(
                resultType,
                this.exchange,
                Endpoint.Account,
                TRANSFER_FUNDS,
                args);
    }

}
