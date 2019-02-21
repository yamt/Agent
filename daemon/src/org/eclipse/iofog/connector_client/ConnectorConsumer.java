package org.eclipse.iofog.connector_client;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.eclipse.iofog.utils.Constants;

import static org.eclipse.iofog.utils.logging.LoggingService.logWarning;

public class ConnectorConsumer {
    public final static String MODULE_NAME = "Connector Consumer";
    private ClientConsumer consumer;
    private ClientConfig config;
    private String name;

    ConnectorConsumer(String producerName, ClientSession session, ClientConfig config) {
        this.name = producerName;
        this.config = config;
        try {
            init(session, config);
        } catch (ActiveMQException e) {
            logWarning(MODULE_NAME, String.format("Connector consumer %s creation error: %s", producerName, e.getMessage()));
        }
    }

    void init(ClientSession session, ClientConfig config) throws ActiveMQException {
        this.consumer = create(session, config.getPublisherId(), config.getPassKey());
        if (consumer != null && !consumer.isClosed()) {
            ConnectorMessageListener listener = new ConnectorMessageListener(new ConnectorMessageCallback());
            consumer.setMessageHandler(listener);
        }
    }

    private ClientConsumer create(ClientSession session, String publisherId, String passKey) throws ActiveMQException {
        ClientConsumer consumer = null;
        if (session != null) {
            consumer = session.createConsumer(
                String.format("%s::%s", Constants.ACTIVEMQ_ADDRESS, publisherId),
                String.format("key='%s'", passKey)
            );
        }
        return consumer;
    }

    public synchronized ClientConfig getConfig() {
        return config;
    }

    public String getName() {
        return name;
    }

    public void close() {
        if (consumer != null && !consumer.isClosed()) {
            try {
                consumer.close();
            } catch (ActiveMQException e) {
                logWarning(MODULE_NAME, String.format("Unable to close connector consumer %s: %s", name, e.getMessage()));
            }
        }
    }

    public synchronized boolean isClosed() {
        return consumer == null || consumer.isClosed();
    }
}
