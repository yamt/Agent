package org.eclipse.iofog.connector_client;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.eclipse.iofog.message_bus.Message;

import static org.eclipse.iofog.utils.logging.LoggingService.logWarning;

public class ConnectorProducer {
    public final static String MODULE_NAME = "Connector Producer";
    private ClientProducer producer;
    private ConnectorClientConfig config;
    private String name;
    private ClientSession session;

    ConnectorProducer(String name, ClientSession session, ConnectorClientConfig connectorProducerConfig) throws ActiveMQException {
        this.name = name;
        this.session = session;
        this.config = connectorProducerConfig;
        this.producer = create(session, connectorProducerConfig.getPublisherId());
    }

    private ClientProducer create(ClientSession session, String publisherId) throws ActiveMQException {
        ClientProducer producer = session.createProducer(String.format("pubsub.iofog.%s", publisherId));
        if (session.isClosed()) {
            session.start();
        }
        return producer;
    }

    public ConnectorClientConfig getConfig() {
        return config;
    }

    public void sendMessage(Message message) {
        ClientMessage msg = session.createMessage(false);
        byte[] bytesMsg = message.getBytes();
        msg.putStringProperty("key", config.getPassKey());
        msg.putBytesProperty("message", bytesMsg);

        if (producer != null) {
            try {
                producer.send(msg);
            } catch (ActiveMQException e) {
                logWarning(MODULE_NAME, "Message sending error: " + e.getMessage());
            }
        } else {
            logWarning(MODULE_NAME, "Producer has not been created");
        }
    }

    public void closeProducer() {
        if (producer != null && !producer.isClosed()) {
            try {
                producer.close();
            } catch (ActiveMQException e) {
                logWarning(MODULE_NAME, "Unable to close connector producer: " + e.getMessage());
            }
        }
    }

    public synchronized boolean isClosed() {
        return producer == null || producer.isClosed();
    }
}
