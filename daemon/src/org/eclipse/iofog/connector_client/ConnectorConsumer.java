package org.eclipse.iofog.connector_client;

import org.apache.activemq.artemis.api.core.client.ClientConsumer;

public class ConnectorConsumer extends ConnectorEntity {
    private ClientConsumer consumer;

    public ConnectorConsumer(String name, ConnectorClient connectorClient, ClientConsumer consumer) {
        super(name, connectorClient);
        this.consumer = consumer;
    }
}
