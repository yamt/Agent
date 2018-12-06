package org.eclipse.iofog.connector_client;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.eclipse.iofog.message_bus.Message;

import static org.eclipse.iofog.utils.logging.LoggingService.logWarning;

public class ConnectorProducer extends ConnectorEntity {
    public final static String MODULE_NAME = "Connector Producer";
    private ClientProducer producer;
    private ConnectorProducerConfig config;

    public ConnectorProducer(String name, ConnectorClient connectorClient, ClientProducer producer, ConnectorProducerConfig connectorProducerConfig) {
        super(name, connectorClient);
        this.producer = producer;
        this.config = connectorProducerConfig;
    }

    public ConnectorProducerConfig getConfig() {
        return config;
    }

    public void sendMessage(Message message) {
        ClientMessage msg = connectorClient.getSession().createMessage(false);
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
        if (!producer.isClosed()) {
            try {
                producer.close();
            } catch (ActiveMQException e) {
                logWarning(MODULE_NAME, "Unable to close connector producer: " + e.getMessage());
            }
        }
    }
}
