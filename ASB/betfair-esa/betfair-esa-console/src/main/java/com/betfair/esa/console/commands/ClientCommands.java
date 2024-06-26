package com.betfair.esa.console.commands;

import com.asbresearch.betfair.esa.Client;
import com.asbresearch.betfair.esa.ClientCache;
import com.asbresearch.betfair.esa.auth.AppKeyAndSessionProvider;
import com.asbresearch.betfair.esa.auth.InvalidCredentialException;
import com.asbresearch.betfair.esa.cache.market.Market;
import com.asbresearch.betfair.esa.cache.market.MarketCache;
import com.asbresearch.betfair.esa.cache.market.MarketChangeEvent;
import com.asbresearch.betfair.esa.cache.market.MarketRunnerPrices;
import com.asbresearch.betfair.esa.cache.market.MarketRunnerSnap;
import com.asbresearch.betfair.esa.cache.market.MarketSnap;
import com.asbresearch.betfair.esa.cache.order.OrderCache;
import com.asbresearch.betfair.esa.cache.order.OrderMarket;
import com.asbresearch.betfair.esa.cache.util.LevelPriceSize;
import com.asbresearch.betfair.esa.cache.util.OrderMarketSnap;
import com.asbresearch.betfair.esa.protocol.ConnectionException;
import com.asbresearch.betfair.esa.protocol.StatusException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.plugin.support.DefaultPromptProvider;
import org.springframework.shell.table.BeanListTableModel;
import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.Table;
import org.springframework.stereotype.Component;


@Component
@Configuration
@Slf4j
public class ClientCommands extends DefaultPromptProvider implements CommandMarker, MarketCache.MarketChangeListener, OrderCache.OrderMarketChangeListener {

    public static final String CONFIG_PROPERTIES = "config.properties";
    public static final String HOST_NAME = "host";
    public static final String PASSWORD = "password";
    public static final String USER_NAME = "userName";
    public static final String APP_KEY = "appKey";
    public static final String DEFAULT_HOST = "stream-api-integration.betfair.com";
    public static final int PORT = 443;

    private Properties properties = new Properties();
    private AppKeyAndSessionProvider sessionProvider;
    private ClientCache clientCache;
    private Client client;
    private boolean traceMarkets = false;
    private boolean traceOrders = false;

    public ClientCommands() throws IOException, InvalidCredentialException {
        loadConfig();

        if (getAppKey() != null) {
            newSessionProvider(getHost(), getAppKey(), getUserName(), getPassword());
            sessionProvider.getOrCreateNewSession();
        }
    }

    private ClientCache getClientCache() {

        if (clientCache == null) {
            if (sessionProvider == null) {
                log.error("No login saved - Use SaveLogin");
                throw new IllegalStateException("No login saved - Use SaveLogin");
            } else {
                client = new Client(DEFAULT_HOST, PORT, sessionProvider);
                clientCache = new ClientCache(client);
                clientCache.getMarketCache().addMarketChangeListener(this);
                clientCache.getOrderCache().addOrderMarketChangeListener(this);
            }
        }

        return clientCache;
    }


    @Override
    public void marketChange(MarketChangeEvent marketChangeEvent) {
        if (traceMarkets) {
            printMarket(marketChangeEvent.getSnap());
        }
    }


    @Override
    public void orderChange(OrderCache.OrderMarketChangeEvent orderChangeEvent) {
        if (traceOrders) {
            printOrderMarket(orderChangeEvent.snap());
        }
    }


    @CliCommand(value = "saveLogin", help = "Save Login - (not encrypted)")
    public void saveLogin(@CliOption(key = {"host"}, specifiedDefaultValue = "identitysso.betfair.com", unspecifiedDefaultValue = "identitysso.betfair.com", help = "sso host") String host,
                          @CliOption(key = {"appKey"}, mandatory = true, help = "app key") String appKey,
                          @CliOption(key = {"userName"}, mandatory = true, help = "user name") String userName,
                          @CliOption(key = {"password"}, mandatory = true, help = "password") String password
    ) throws IOException, InvalidCredentialException {
        properties.setProperty(APP_KEY, appKey);
        properties.setProperty(USER_NAME, userName);
        properties.setProperty(PASSWORD, password);
        properties.setProperty(HOST_NAME, host);

        //set the login and verify before saving
        newSessionProvider(host, appKey, userName, password);
        //test it
        sessionProvider.getOrCreateNewSession();

        //save it
        properties.store(new FileOutputStream(CONFIG_PROPERTIES), "");
        System.out.println("saveLogin - saved credentials (un-encrypted) to: "+CONFIG_PROPERTIES);
    }


    @CliCommand(value = "market", help = "subscribes to market(s) (comma separated with no spaces)")
    public void market(@CliOption(key = {""}, mandatory = true, help = "marketId") String... marketId) throws ConnectionException, StatusException, InvalidCredentialException {
        getClientCache().subscribeMarkets(marketId);
    }

    @CliCommand(value = "marketFirehose", help = "subscribes to all markets")
    public void marketFirehose() throws ConnectionException, StatusException, InvalidCredentialException {
        getClientCache().subscribeMarkets();
    }


    @CliCommand(value = "orders", help = "subscribes to orders")
    public void orders() throws ConnectionException, StatusException, InvalidCredentialException {
        getClientCache().subscribeOrders();
    }

    @CliCommand(value = "listMarkets", help = "lists the cached markets")
    public void listMarkets() {
        for (Market market : getClientCache().getMarketCache().getMarkets()) {
            printMarket(market.getSnap());
        }
    }

    @CliCommand(value = "listOrders", help = "lists the cached orders")
    public void listOrders() {
        for (OrderMarket orderMarket : getClientCache().getOrderCache().getOrderMarkets()) {
            printOrderMarket(orderMarket.getOrderMarketSnap());
        }
    }

    @CliCommand(value = "traceMarkets", help = "trace Markets")
    public void traceMarkets() {
        traceMarkets = true;
    }

    @CliCommand(value = "traceOrders", help = "trace Orders")
    public void traceOrders() {
        traceOrders = true;
    }


    @CliCommand(value = "stop", help = "stops the client")
    public void stop() {
        client.stop();
    }

    @CliCommand(value = "start", help = "starts the client")
    public void start() throws ConnectionException, StatusException, InvalidCredentialException {
        client.start();
    }

    @CliCommand(value = "disconnect", help = "socket level disconnect - this will auto-reconnect")
    public void disconnect() {
        client.disconnect();
    }


    @CliCommand(value = "traceMessages", help = "trace Messages (Markets and Orders)")
    public void traceMessages(@CliOption(key = {""}, mandatory = false, help = "truncate", unspecifiedDefaultValue = "200", specifiedDefaultValue = "200") int truncate) {
        client.setTraceChangeTruncation(truncate);
    }

    private void printMarket(MarketSnap market) {
        market.getMarketRunners().sort((mr1, mr2) -> Integer.compare(mr1.getDefinition().getSortPriority(), mr2.getDefinition().getSortPriority()));

        List<MarketDetailsRow> marketDetails = new ArrayList<>();

        for (MarketRunnerSnap runner : market.getMarketRunners()) {
            MarketRunnerPrices snap = runner.getPrices();
            MarketDetailsRow marketDetail = new MarketDetailsRow(market.getMarketId(),
                    runner.getRunnerId().getSelectionId(),
                    getLevel(snap.getBdatb(), 0).getPrice(),
                    getLevel(snap.getBdatb(), 0).getSize(),
                    getLevel(snap.getBdatl(), 0).getPrice(),
                    getLevel(snap.getBdatl(), 0).getSize());
            marketDetails.add(marketDetail);
        }

        BeanListTableModel model = new BeanListTableModel(marketDetails, createHeader("marketId", "selectionId", "batbPrice", "batbSize", "batlPrice", "batlSize"));
        renderTable(model);
    }


    private void printOrderMarket(OrderMarketSnap orderMarketSnap) {
        System.out.println("Orders  (marketid=" + orderMarketSnap.getMarketId() + ")");

        List<com.betfair.esa.swagger.model.Order> orders = new ArrayList<>();

        orderMarketSnap.getOrderMarketRunners().forEach(orderMarketRunnerSnap -> {
            orders.addAll(orderMarketRunnerSnap.getUnmatchedOrders().values());
        });

        BeanListTableModel model = new BeanListTableModel(orders, createHeader( "id","side", "pt","ot", "status","sv","p","sc","rc","s","pd","rac","md","sl","avp","sm","bsp","sr"));
        renderTable(model);
    }

    private void renderTable(BeanListTableModel model) {
        org.springframework.shell.table.TableBuilder tableBuilder = new org.springframework.shell.table.TableBuilder(model);
        tableBuilder.addHeaderAndVerticalsBorders(BorderStyle.oldschool);
        Table table = tableBuilder.build();
        final String rendered = table.render(1000);
        System.out.println(rendered);
    }

    private LinkedHashMap<String, Object> createHeader(String... headers) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        for (String header : headers) {
            result.put(header, header);
        }
        return result;
    }

    private static LevelPriceSize getLevel(List<LevelPriceSize> values, int level) {
        return !values.isEmpty() ? values.get(0) : new LevelPriceSize(level, 0, 0);
    }

    private void loadConfig() {
        InputStream input = null;
        try {
            File configFile = new File(CONFIG_PROPERTIES);
            if (!configFile.exists()) {
                configFile.createNewFile();

            }
            input = new FileInputStream(CONFIG_PROPERTIES);
            if (input != null) {
                properties.load(input);
            }

        } catch (IOException ex) {
            log.error(ex.getMessage());
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void newSessionProvider(String ssoHost, String appKey, String userName, String password) {
        sessionProvider = new AppKeyAndSessionProvider(ssoHost, appKey, userName, password);
    }


    private String getAppKey() {
        return properties.getProperty(APP_KEY);
    }

    private String getPassword() {
        return properties.getProperty(PASSWORD);
    }

    private String getUserName() {
        return properties.getProperty(USER_NAME);
    }

    private String getHost() {
        return properties.getProperty(HOST_NAME);
    }

    class MarketDetailsRow {
        private String marketId;
        private long selectionId;
        private double batbPrice;
        private double batbSize;
        private double batlPrice;
        private double batlSize;


        public MarketDetailsRow(String marketId, long selectionId, double batbPrice, double batbSize, double batlPrice, double batlSize) {
            this.marketId = marketId;
            this.selectionId = selectionId;
            this.batbPrice = batbPrice;
            this.batbSize = batbSize;
            this.batlPrice = batlPrice;
            this.batlSize = batlSize;
        }

        public String getMarketId() {
            return marketId;
        }

        public long getSelectionId() {
            return selectionId;
        }

        public double getBatbPrice() {
            return batbPrice;
        }

        public double getBatbSize() {
            return batbSize;
        }

        public double getBatlPrice() {
            return batlPrice;
        }

        public double getBatlSize() {
            return batlSize;
        }
    }
}


