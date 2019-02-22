package org.eclipse.iofog.message_bus;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.eclipse.iofog.connector_client.ConnectorManager;
import org.eclipse.iofog.connector_client.ConnectorProducer;
import org.eclipse.iofog.local_api.RemoteMessageCallback;
import org.eclipse.iofog.microservice.Receiver;

import static org.eclipse.iofog.utils.logging.LoggingService.logError;

public class RemoteMessageReceiver extends MessageReceiver {

    private static final String MODULE_NAME = "Remote Message Receiver";

    private ConnectorProducer connectorProducer;

    public RemoteMessageReceiver(Receiver receiver, ClientConsumer consumer) {
        super(receiver, consumer);
        enableConnectorProducing();
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    synchronized ConnectorProducer getConnectorProducer() {
        return connectorProducer;
    }

    synchronized void enableConnectorProducing() {
        if (consumer != null && !consumer.isClosed()) {
            connectorProducer = ConnectorManager.INSTANCE.getProducer(receiver.getMicroserviceUuid(), receiver.getConnectorProducerConfig());
            if (connectorProducer != null && !connectorProducer.isClosed()) {
                listener = new MessageListener(new RemoteMessageCallback(
                    receiver.getConnectorProducerConfig().getPublisherId(),
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

    @Override
    public synchronized void update(Receiver receiver) {
        if (!this.receiver.getConnectorProducerConfig().equals(receiver.getConnectorProducerConfig())) {
            disableConnectorProducing();
            this.receiver = receiver;
            enableConnectorProducing();
        }
    }

    @Override
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
