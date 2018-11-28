package org.eclipse.iofog.connector_client;

import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.eclipse.iofog.message_bus.Message;

public class ConnectorMessageListener implements MessageHandler {
    private static final String MODULE_NAME = "Connector Message Listener";
    private final ConnectorMessageCallback callback;

    public ConnectorMessageListener(ConnectorMessageCallback connectorMessageCallback) {
        this.callback = connectorMessageCallback;
    }

    @Override
    public void onMessage(ClientMessage msg) {
        Message message = new Message(msg.getBytesProperty("message"));
        callback.sendConnectorMessage(message);
    }
}
