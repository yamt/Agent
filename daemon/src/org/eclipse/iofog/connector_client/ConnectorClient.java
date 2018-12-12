package org.eclipse.iofog.connector_client;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.*;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.eclipse.iofog.utils.configuration.Configuration;

import java.util.HashMap;
import java.util.Map;

import static org.eclipse.iofog.utils.logging.LoggingService.logWarning;

public class ConnectorClient {
    public final static String MODULE_NAME = "Connector Client";
    private int id;
    private ConnectorConfig connectorConfig;
    private ClientSession session;

    public ConnectorClient(int id, ConnectorConfig connectorConfig) {
        this.id = id;
        this.connectorConfig = connectorConfig;
    }

    ClientSession getSession() {
        return session;
    }

    public int getId() {
        return id;
    }

    ConnectorConfig getConnectorConfig() {
        return connectorConfig;
    }

    void createSession() throws Exception {
        Map<String, Object> connectionParams = connectorConfig.isDevModeEnabled()
            ? getDevModeConnectionParams()
            : getNonDevModeConnectionParams();

        TransportConfiguration transportConfiguration =
            new TransportConfiguration(
                NettyConnectorFactory.class.getName(),
                connectionParams);

        ServerLocator locator = ActiveMQClient.createServerLocatorWithoutHA(transportConfiguration);
        ClientSessionFactory factory = locator.createSessionFactory();
        session = factory.createSession(connectorConfig.getUser(), connectorConfig.getPassword(), false, true, true, false, 0);
    }

    ClientProducer createProducer(String topicName) throws ActiveMQException {
        return session.createProducer(String.format("pubsub.iofog.%s", topicName));
    }

    ClientConsumer createConsumer(String topicName, String passKey) throws ActiveMQException {
        return session.createConsumer(String.format("pubsub.iofog.%s", topicName), String.format("key='%s'", passKey));
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

    boolean isClosed() {
        return session == null || session.isClosed();
    }

    private Map<String, Object> getNonDevModeConnectionParams() {
        Map<String, Object> connectionParams = new HashMap<>();
        connectionParams.put(TransportConstants.PORT_PROP_NAME, connectorConfig.getPort());
        connectionParams.put(TransportConstants.HOST_PROP_NAME, connectorConfig.getHost());
        return connectionParams;
    }

    private Map<String, Object> getDevModeConnectionParams() {
        Map<String, Object> connectionParams = new HashMap<>();
        connectionParams.put(TransportConstants.PORT_PROP_NAME, connectorConfig.getPort());
        connectionParams.put(TransportConstants.HOST_PROP_NAME, connectorConfig.getHost());
        connectionParams.put(TransportConstants.SSL_ENABLED_PROP_NAME, true);
        connectionParams.put(TransportConstants.TRUSTSTORE_PATH_PROP_NAME, Configuration.getConnectorTruststore());
        connectionParams.put(TransportConstants.TRUSTSTORE_PASSWORD_PROP_NAME, Configuration.getConnectorTruststorePassword());
        return connectionParams;
    }
}
