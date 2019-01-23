package org.eclipse.iofog.connector_client;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;

import java.util.HashMap;
import java.util.Map;

class ClientSessions {
    static ClientSession defaultAuthenticatedSession(ClientSessionFactory sf,
                                                     String username,
                                                     String password) throws ActiveMQException {
        return createSession(sf, username, password, false, true, true,
            false, 0);
    }



    static ClientSessionFactory createSessionFactory(String host,
                                                     int port,
                                                     String truststorePath,
                                                     String truststorePassword) throws Exception {
        Map<String, Object> connectionParams = getSslConnectionParams(host, port, truststorePath, truststorePassword);
        return createSessionFactory(connectionParams);
    }

    static ClientSessionFactory createSessionFactory(String host, int port) throws Exception {
        Map<String, Object> connectionParams = getNonSslConnectionParams(host, port);
        return createSessionFactory(connectionParams);
    }

    private static ClientSession createSession(ClientSessionFactory csf,
                                               String username,
                                               String password,
                                               boolean xa,
                                               boolean autoCommitSends,
                                               boolean autoCommitAcks,
                                               boolean preAcknowledge,
                                               int ackBatchSize)
        throws ActiveMQException {
        return csf.createSession(username, password, xa, autoCommitSends,
            autoCommitAcks, preAcknowledge, ackBatchSize);
    }

    private static ClientSessionFactory createSessionFactory(Map<String, Object> connectionParams) throws Exception {
        TransportConfiguration transportConfiguration =
            new TransportConfiguration(NettyConnectorFactory.class.getName(), connectionParams);

        ServerLocator locator = ActiveMQClient.createServerLocatorWithoutHA(transportConfiguration);
        return locator.createSessionFactory();
    }

    private static Map<String, Object> getSslConnectionParams(String host,
                                                              int port,
                                                              String trustorePath,
                                                              String trustorePassword) {
        Map<String, Object> connectionParams = new HashMap<>();
        connectionParams.put(TransportConstants.HOST_PROP_NAME, host);
        connectionParams.put(TransportConstants.PORT_PROP_NAME, port);
        connectionParams.put(TransportConstants.SSL_ENABLED_PROP_NAME, true);
        connectionParams.put(TransportConstants.TRUSTSTORE_PATH_PROP_NAME, trustorePath);
        connectionParams.put(TransportConstants.TRUSTSTORE_PASSWORD_PROP_NAME, trustorePassword);
        return connectionParams;
    }

    private static Map<String, Object> getNonSslConnectionParams(String host, int port) {
        Map<String, Object> connectionParams = new HashMap<>();
        connectionParams.put(TransportConstants.HOST_PROP_NAME, host);
        connectionParams.put(TransportConstants.PORT_PROP_NAME, port);
        return connectionParams;
    }
}
