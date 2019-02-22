package org.eclipse.iofog.message_bus;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.eclipse.iofog.local_api.MessageCallback;
import org.eclipse.iofog.microservice.Receiver;

import static org.eclipse.iofog.utils.logging.LoggingService.logError;

public class LocalMessageReceiver extends MessageReceiver {
    private static final String MODULE_NAME = "Local Message Receiver";

    LocalMessageReceiver(Receiver receiver, ClientConsumer consumer) {
        super(receiver, consumer);
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    /**
     * enables real-time receiving for this {@link org.eclipse.iofog.microservice.Microservice}
     */
    void enableRealTimeReceiving() {
        if (consumer == null || consumer.isClosed())
            return;
        listener = new MessageListener(new MessageCallback(receiver.getMicroserviceUuid()));
        try {
            consumer.setMessageHandler(listener);
        } catch (ActiveMQException e) {
            logError(MODULE_NAME, "Unable to set message bus handler: " + e.getMessage(), e);
        }
    }

    /**
     * disables real-time receiving for this {@link org.eclipse.iofog.microservice.Microservice}
     */
    void disableRealTimeReceiving() {
        try {
            if (consumer == null || listener == null || consumer.getMessageHandler() == null)
                return;
            listener = null;
            consumer.setMessageHandler(null);
        } catch (Exception exp) {
            logError(MODULE_NAME, exp.getMessage(), exp);
        }
    }

    public synchronized void close() {
        if (consumer == null)
            return;
        disableRealTimeReceiving();
        try {
            consumer.close();
        } catch (Exception exp) {
            logError(MODULE_NAME, exp.getMessage(), exp);
        }
    }

    @Override
    public void update(Receiver receiver) {}
}
