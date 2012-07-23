/**
 * Mule Salesforce Connector
 *
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.modules.salesforce;

import com.sforce.soap.partner.LoginResult;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectorConfig;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.transport.ClientTransport;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.MalformedURLException;
import java.net.ProtocolException;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SalesforceBayeuxClientTest {
    @Test
    public void verifyTimeOut() throws Exception {
        SalesforceBayeuxClient bayeuxClient = mockBayeuxClient();

        assertEquals(bayeuxClient.LONG_POLLING_OPTIONS.get(ClientTransport.TIMEOUT_OPTION), bayeuxClient.LONG_POLLING_TIMEOUT);
    }

    @Test
    public void verifyCookies() throws Exception {
        SalesforceBayeuxClient bayeuxClient = mockBayeuxClient();

        assertEquals(bayeuxClient.getCookie(SalesforceBayeuxClient.LOCALEINFO_COOKIE), "us");
        assertEquals(bayeuxClient.getCookie(SalesforceBayeuxClient.LOGIN_COOKIE), "mulesoft");
        assertEquals(bayeuxClient.getCookie(SalesforceBayeuxClient.SESSIONID_COOKIE), "001");
        assertEquals(bayeuxClient.getCookie(SalesforceBayeuxClient.LANGUAGE_COOKIE), "en_US");
    }

    private SalesforceBayeuxClient mockBayeuxClient() throws MalformedURLException {
        PartnerConnection connection = Mockito.mock(PartnerConnection.class);
        LoginResult loginResult = Mockito.mock(LoginResult.class);
        ConnectorConfig connectorConfig = Mockito.mock(ConnectorConfig.class);
        SalesforceConnector connector = Mockito.mock(SalesforceConnector.class);
        when(connector.getConnection()).thenReturn(connection);
        when(connector.getLoginResult()).thenReturn(loginResult);
        when(connector.getSessionId()).thenReturn("001");
        when(connection.getConfig()).thenReturn(connectorConfig);
        when(connectorConfig.getServiceEndpoint()).thenReturn("http://xxx.salesforce.com");
        when(connectorConfig.getUsername()).thenReturn("mulesoft");
        return new SalesforceBayeuxClient(connector);
    }

    @Test
    public void verifySubscriptionCache() throws Exception {
        SalesforceBayeuxClient bayeuxClient = mockBayeuxClient();
        ClientSessionChannel.MessageListener messageListener = Mockito.mock(ClientSessionChannel.MessageListener.class);

        bayeuxClient.subscribe("channel", messageListener);

        assertEquals(bayeuxClient.subscriptions.get("channel"), messageListener);
    }
    
    @Test
    public void verifySubscription() throws Exception {
        SalesforceBayeuxClient bayeuxClient = spy(mockBayeuxClient());
        ClientSessionChannel.MessageListener messageListener = Mockito.mock(ClientSessionChannel.MessageListener.class);
        ClientSessionChannel clientSessionChannel = Mockito.mock(ClientSessionChannel.class);
        doReturn(true).when(bayeuxClient).isConnected();
        doReturn(clientSessionChannel).when(bayeuxClient).getChannel("channel");

        bayeuxClient.subscribe("channel", messageListener);

        verify(clientSessionChannel, times(1)).subscribe(messageListener);
    }    

    @Test
    public void verifyOnFailureReconnect() throws Exception {
        SalesforceBayeuxClient bayeuxClient = spy(mockBayeuxClient());
        ProtocolException protocolException = Mockito.mock(ProtocolException.class);
        doNothing().when(bayeuxClient).handshake();
        
        bayeuxClient.onFailure(protocolException, new Message[]{});

        verify(bayeuxClient.salesforceConnector, times(1)).reconnect();

    }

}
