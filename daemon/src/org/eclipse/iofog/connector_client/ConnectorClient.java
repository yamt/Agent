package org.eclipse.iofog.connector_client;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.*;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyAcceptorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;

import java.util.HashMap;
import java.util.Map;

public class ConnectorClient {
    public final static String MODULE_NAME = "Connector Client";
    private Integer id;
    private ConnectorConfig routeConfig;
    private ClientSession session;

    public ClientSession getSession() {
        return session;
    }

    public Integer getId() {
        return id;
    }

    public ConnectorConfig getRouteConfig() {
        return routeConfig;
    }

    public void createSession() throws Exception {
        Map<String, Object> connectionParams = new HashMap<>();
        connectionParams.put(TransportConstants.PORT_PROP_NAME, routeConfig.getPort());
        connectionParams.put(TransportConstants.HOST_PROP_NAME, routeConfig.getHost());

        TransportConfiguration transportConfiguration =
            new TransportConfiguration(
                NettyAcceptorFactory.class.getName(),
                connectionParams);

        ServerLocator locator = ActiveMQClient.createServerLocatorWithoutHA(transportConfiguration);
        ClientSessionFactory factory = locator.createSessionFactory();
        session = factory.createSession(routeConfig.getUser(), routeConfig.getPassword(), false, true, true, false, 0);
    }

    public ClientProducer createProducer() throws ActiveMQException {
        return session.createProducer();
    }

    public ClientConsumer createConsumer(String topicName, String filter) throws ActiveMQException {
        return session.createConsumer(topicName, filter);
    }

    public void startSession() throws ActiveMQException {
        session.start();
    }
}
