package org.eclipse.iofog.connector_client;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.MessageHandler;

import static org.eclipse.iofog.utils.logging.LoggingService.logWarning;

public class ConnectorConsumer {
    public final static String MODULE_NAME = "Connector Consumer";
    private ClientConsumer consumer;
    private ConnectorClientConfig config;
    private String name;
    private ClientSession session;

    ConnectorConsumer(String name, ClientSession session, ConnectorClientConfig config) throws ActiveMQException {
        this.name = name;
        this.session = session;
        this.config = config;
        this.consumer = create(session, config.getPublisherId(), config.getPassKey());
    }

    private ClientConsumer create(ClientSession session, String publisherId, String passKey) throws ActiveMQException {
        ClientConsumer consumer = session.createConsumer(
            String.format("pubsub.iofog.%s", publisherId),
            String.format("key='%s'", passKey)
        );
        if (session.isClosed()) {
            session.start();
        }
        return consumer;
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
