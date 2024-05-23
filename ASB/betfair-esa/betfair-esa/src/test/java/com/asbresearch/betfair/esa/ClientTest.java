package com.asbresearch.betfair.esa;

import com.asbresearch.betfair.esa.auth.InvalidCredentialException;
import com.asbresearch.betfair.esa.protocol.ConnectionException;
import com.asbresearch.betfair.esa.protocol.ConnectionStatus;
import com.asbresearch.betfair.esa.protocol.StatusException;
import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ClientTest extends BaseTest {

    private Client client;

    @Before
    public void beforeMethod() {
        client = new Client("stream-api-integration.betfair.com", 443, getValidSessionProvider());
    }

    @After
    public void afterMethod() {
        client.stop();
    }

    @Test(expected = ConnectionException.class)
    public void testInvalidHost() throws InvalidCredentialException, StatusException, ConnectionException {
        Client invalidClient = new Client("www.betfair.com", 443, getValidSessionProvider());
        invalidClient.setTimeout(100);
        invalidClient.start();
    }

    @Test
    public void testStartStop() throws InvalidCredentialException, StatusException, ConnectionException {
        Assert.assertEquals(client.getStatus(), ConnectionStatus.STOPPED);
        client.start();
        Assert.assertEquals(client.getStatus(), ConnectionStatus.AUTHENTICATED);
        client.stop();
        Assert.assertEquals(client.getStatus(), ConnectionStatus.STOPPED);
    }

    @Test
    public void testStartHeartbeatStop() throws InvalidCredentialException, StatusException, ConnectionException {
        client.start();
        client.heartbeat();
        client.stop();
    }

    @Test
    public void testReentrantStartStop() throws InvalidCredentialException, StatusException, ConnectionException {
        client.start();
        Assert.assertEquals(client.getStatus(), ConnectionStatus.AUTHENTICATED);
        client.heartbeat();
        client.stop();
        Assert.assertEquals(client.getStatus(), ConnectionStatus.STOPPED);

        client.start();
        Assert.assertEquals(client.getStatus(), ConnectionStatus.AUTHENTICATED);
        client.heartbeat();
        client.stop();
        Assert.assertEquals(client.getStatus(), ConnectionStatus.STOPPED);
    }

    @Test
    public void testDoubleStartStop() throws InvalidCredentialException, StatusException, ConnectionException {
        client.start();
        client.start();
        Assert.assertEquals(client.getStatus(), ConnectionStatus.AUTHENTICATED);
        client.heartbeat();
        client.stop();
        client.stop();
        Assert.assertEquals(client.getStatus(), ConnectionStatus.STOPPED);
    }

    @Test
    public void testDisconnectWithAutoReconnect() throws InvalidCredentialException, StatusException, ConnectionException {
        client.start();
        Assert.assertEquals(client.getStatus(), ConnectionStatus.AUTHENTICATED);
        client.heartbeat();

        //socket disconnect
        Assert.assertEquals(client.getDisconnectCounter(), 0);
        client.disconnect();

        //retry until connected
        Awaitility.await().catchUncaughtExceptions().atMost(Duration.ONE_MINUTE).until(() -> {
            try {
                client.heartbeat();
                return true;
            } catch (Throwable e) {
                return false;
            }
        });
        Assert.assertEquals(client.getStatus(), ConnectionStatus.AUTHENTICATED);
        Assert.assertEquals(client.getDisconnectCounter(), 1);
    }
}
