package org.eclipse.iofog.connector_client;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.*;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;

import java.util.HashMap;
import java.util.Map;

import static org.eclipse.iofog.utils.logging.LoggingService.logWarning;

public class ConnectorClient {
    public final static String MODULE_NAME = "Connector Client";
    private Integer id;
    private ConnectorConfig connectorConfig;
    private ClientSession session;

    public ConnectorClient(Integer id, ConnectorConfig connectorConfig) {
        this.id = id;
        this.connectorConfig = connectorConfig;
    }

    synchronized ClientSession getSession() {
        return session;
    }

    public Integer getId() {
        return id;
    }

    ConnectorConfig getConnectorConfig() {
        return connectorConfig;
    }

    void createSession() throws Exception {
        Map<String, Object> connectionParams = new HashMap<>();
        connectionParams.put(TransportConstants.PORT_PROP_NAME, connectorConfig.getPort());
        connectionParams.put(TransportConstants.HOST_PROP_NAME, connectorConfig.getHost());

        TransportConfiguration transportConfiguration =
            new TransportConfiguration(
                NettyConnectorFactory.class.getName(),
                connectionParams);

        ServerLocator locator = ActiveMQClient.createServerLocatorWithoutHA(transportConfiguration);
        ClientSessionFactory factory = locator.createSessionFactory();
        session = factory.createSession(connectorConfig.getUser(), connectorConfig.getPassword(), false, true, true, false, 0);
    }

    ClientProducer createProducer() throws ActiveMQException {
        return session.createProducer();
    }

    ClientConsumer createConsumer(String topicName, String filter) throws ActiveMQException {
        return session.createConsumer(topicName, filter);
    }

    void startSession() {
        try {
            session.start();
        } catch (ActiveMQException e) {
            logWarning(MODULE_NAME, "Unable to start connector session: " + e.getMessage());
        }
    }

    void closeSession() {
        if (!session.isClosed()) {
            try {
                session.close();
            } catch (ActiveMQException e) {
                logWarning(MODULE_NAME, "Unable to close connector session: " + e.getMessage());
            }
        }
    }
}
