package com.asbresearch.betfair.esa;

import com.asbresearch.betfair.esa.auth.AppKeyAndSessionProvider;
import com.asbresearch.betfair.esa.auth.InvalidCredentialException;
import com.asbresearch.betfair.esa.protocol.ConnectionException;
import com.asbresearch.betfair.esa.protocol.StatusException;
import org.junit.Test;

public class ClientCacheTest extends BaseTest {

    @Test
    public void testUserStory() throws InvalidCredentialException, StatusException, ConnectionException, InterruptedException {
        //1: Create a session provider
        AppKeyAndSessionProvider sessionProvider = new AppKeyAndSessionProvider(
                AppKeyAndSessionProvider.SSO_HOST_COM,
                getAppKey(),
                getUserName(),
                getPassword());

        //2: Create a client
        Client client = new Client(
                "stream-api-integration.betfair.com",
                443,
                sessionProvider);

        //3: Create a cache
        ClientCache cache = new ClientCache(client);

        //4: Setup order subscription
        //Register for change events
//        cache.getOrderCache().OrderMarketChanged +=
//                (sender, arg) => Console.WriteLine("Order:" + arg.Snap);
        //Subscribe to orders
        cache.subscribeOrders();

        //5: Setup market subscription
        //Register for change events
        cache.getMarketCache().addMarketChangeListener(e -> System.out.println("Market:" + e.getSnap()));
//        cache.MarketCache.MarketChanged +=
//                (sender, arg) => Console.WriteLine("Market:" + arg.Snap);
        //Subscribe to markets (use a valid market id or filter)
        cache.subscribeMarkets("1.125499232");

        Thread.sleep(5000); //pause for a bit
    }

}