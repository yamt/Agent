package org.eclipse.iofog.connector_client;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientSession;

public interface SessionPool {

    ClientSession getSession() throws Exception;

    boolean releaseSession(ClientSession session) throws ActiveMQException;

    void shutdown() throws ActiveMQException;

    ConnectorConfig getConnectorConfig();

    int getSize();
}
