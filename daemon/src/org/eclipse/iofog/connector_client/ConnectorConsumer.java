package org.eclipse.iofog.connector_client;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.MessageHandler;

import static org.eclipse.iofog.utils.logging.LoggingService.logWarning;

public class ConnectorConsumer extends ConnectorEntity {
    public final static String MODULE_NAME = "Connector Consumer";
    private ClientConsumer consumer;
    private ConnectorClientConfig config;

    ConnectorConsumer(String name, ConnectorClient connector, ClientConsumer consumer, ConnectorClientConfig config) {
        super(name, connector);
        this.consumer = consumer;
        this.config = config;
    }

    public ConnectorClientConfig getConfig() {
        return config;
    }

    public void setMessageListener(MessageHandler handler) {
        try {
            consumer.setMessageHandler(handler);
        } catch (ActiveMQException e) {
            logWarning(MODULE_NAME, "Unable to set connector message handler: " + e.getMessage());
        }
    }

    public void closeConsumer() {
        if (consumer != null && !consumer.isClosed()) {
            try {
                consumer.close();
            } catch (ActiveMQException e) {
                logWarning(MODULE_NAME, "Unable to close connector consumer: " + e.getMessage());
            }
        }
    }

    public synchronized boolean isClosed() {
        return consumer == null || consumer.isClosed();
    }
}
