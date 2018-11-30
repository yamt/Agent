package org.eclipse.iofog.local_api;

import org.eclipse.iofog.connector_client.ConnectorClient;
import org.eclipse.iofog.message_bus.Message;

import static org.eclipse.iofog.utils.logging.LoggingService.logInfo;

public class RemoteMessageCallback extends MessageCallback {

	private ConnectorClient connectorClient;

	public RemoteMessageCallback(String name, ConnectorClient connectorClient) {
		super(name);
		this.connectorClient = connectorClient;
		this.connectorClient.createProducer();
	}

	@Override
	public void sendRealtimeMessage(Message message) {
		connectorClient.sendRealTimeMessage(message);
		logInfo("MessageCallback", "send message to connector: " + message.toString());
	}
}
