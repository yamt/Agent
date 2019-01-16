package org.eclipse.iofog.connector_client;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.eclipse.iofog.utils.configuration.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConnectorSessionPool implements SessionPool {

    private static final int INITIAL_POOL_SIZE = 5;
    private final ConnectorConfig connectorConfig;
    private final List<ClientSession> sessionPool;
    private final List<ClientSession> usedSessions = new ArrayList<>();

    public static ConnectorSessionPool create(ConnectorConfig connectorConfig) throws Exception {
        List<ClientSession> pool = new ArrayList<>(INITIAL_POOL_SIZE);
        for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
            pool.add(createSession(connectorConfig));
        }
        return new ConnectorSessionPool(connectorConfig, pool);
    }

    private ConnectorSessionPool(ConnectorConfig connectorConfig, List<ClientSession> sessionPool) {
        this.connectorConfig = connectorConfig;
        this.sessionPool = sessionPool;
    }

    @Override
    public ClientSession getSession() throws Exception {
        if (sessionPool.isEmpty()) {
            sessionPool.add(createSession(connectorConfig));
        }

        ClientSession session = sessionPool.remove(sessionPool.size() - 1);
        usedSessions.add(session);
        return session;
    }

    @Override
    public boolean releaseSession(ClientSession session) throws ActiveMQException {
        if (sessionPool.size() < INITIAL_POOL_SIZE) {
            sessionPool.add(session);
        } else {
            if (!session.isClosed()) {
                session.close();
            }
        }
        return usedSessions.remove(session);
    }

    @Override
    public void shutdown() throws ActiveMQException {
        for (ClientSession session : usedSessions) {
            releaseSession(session);
        }
        for (ClientSession session : sessionPool) {
            if (!session.isClosed()) {
                session.close();
            }
        }
        sessionPool.clear();
    }

    @Override
    public ConnectorConfig getConnectorConfig() {
        return connectorConfig;
    }

    @Override
    public int getSize() {
        return sessionPool.size() + usedSessions.size();
    }

    private static ClientSession createSession(ConnectorConfig connectorConfig) throws Exception {
        Map<String, Object> connectionParams = connectorConfig.isDevModeEnabled()
            ? getDevModeConnectionParams(connectorConfig)
            : getNonDevModeConnectionParams(connectorConfig);

        TransportConfiguration transportConfiguration =
            new TransportConfiguration(
                NettyConnectorFactory.class.getName(),
                connectionParams);

        ServerLocator locator = ActiveMQClient.createServerLocatorWithoutHA(transportConfiguration);
        ClientSessionFactory factory = locator.createSessionFactory();
        return factory.createSession(connectorConfig.getUser(), connectorConfig.getPassword(), false, true, true, false, 0);
    }

    private static Map<String, Object> getNonDevModeConnectionParams(ConnectorConfig connectorConfig) {
        Map<String, Object> connectionParams = new HashMap<>();
        connectionParams.put(TransportConstants.PORT_PROP_NAME, connectorConfig.getPort());
        connectionParams.put(TransportConstants.HOST_PROP_NAME, connectorConfig.getHost());
        return connectionParams;
    }

    private static Map<String, Object> getDevModeConnectionParams(ConnectorConfig connectorConfig) {
        Map<String, Object> connectionParams = new HashMap<>();
        connectionParams.put(TransportConstants.PORT_PROP_NAME, connectorConfig.getPort());
        connectionParams.put(TransportConstants.HOST_PROP_NAME, connectorConfig.getHost());
        connectionParams.put(TransportConstants.SSL_ENABLED_PROP_NAME, true);
        connectionParams.put(TransportConstants.TRUSTSTORE_PATH_PROP_NAME, Configuration.getConnectorTruststore());
        connectionParams.put(TransportConstants.TRUSTSTORE_PASSWORD_PROP_NAME, Configuration.getConnectorTruststorePassword());
        return connectionParams;
    }
}
