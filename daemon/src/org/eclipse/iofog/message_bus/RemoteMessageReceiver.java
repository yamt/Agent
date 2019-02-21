package org.eclipse.iofog.message_bus;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.eclipse.iofog.connector_client.ClientConfig;
import org.eclipse.iofog.connector_client.ConnectorManager;
import org.eclipse.iofog.connector_client.ConnectorProducer;
import org.eclipse.iofog.local_api.RemoteMessageCallback;

import static org.eclipse.iofog.utils.logging.LoggingService.logError;

public class RemoteMessageReceiver extends MessageReceiver {

    private static final String MODULE_NAME = "Remote Message Receiver";

    private ClientConfig connectorProducerConfig;
    private ConnectorProducer connectorProducer;

    public RemoteMessageReceiver(ClientConfig connectorProducerConfig, ClientConsumer consumer) {
        super(consumer);
        this.connectorProducerConfig = connectorProducerConfig;
        enableConnectorProducing();
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    public synchronized ClientConfig getConnectorProducerConfig() {
        return connectorProducerConfig;
    }

    synchronized ConnectorProducer getConnectorProducer() {
        return connectorProducer;
    }

    synchronized void enableConnectorProducing() {
        if (consumer != null && !consumer.isClosed()) {
            connectorProducer = ConnectorManager.INSTANCE.getProducer(connectorProducerConfig);
            if (connectorProducer != null && !connectorProducer.isClosed()) {
                listener = new MessageListener(new RemoteMessageCallback(
                    connectorProducerConfig.getPublisherId(),
                    connectorProducer)
                );
                try {
                    consumer.setMessageHandler(listener);
                } catch (ActiveMQException e) {
                    logError(MODULE_NAME, "Unable to set message bus handler: " + e.getMessage(), e);
                }
            }
        }
    }

    private void disableConnectorProducing() {
        if (connectorProducer != null) {
            ConnectorManager.INSTANCE.removeProducer(connectorProducer.getName());
        }
    }

    synchronized void update(ClientConfig connectorProducerConfig) {
        if (!this.connectorProducerConfig.equals(connectorProducerConfig)) {
            disableConnectorProducing();
            this.connectorProducerConfig = connectorProducerConfig;
            enableConnectorProducing();
        }
    }

    public synchronized void close() {
        if (consumer == null)
            return;
        disableConnectorProducing();
        try {
            consumer.close();
        } catch (Exception exp) {
            logError(MODULE_NAME, exp.getMessage(), exp);
        }
    }
}
