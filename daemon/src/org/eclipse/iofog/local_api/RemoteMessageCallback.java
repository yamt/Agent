package org.eclipse.iofog.local_api;

import org.eclipse.iofog.connector_client.ConnectorClient;
import org.eclipse.iofog.connector_client.ConnectorClientOld;
import org.eclipse.iofog.connector_client.ConnectorProducer;
import org.eclipse.iofog.message_bus.Message;

import static org.eclipse.iofog.utils.logging.LoggingService.logInfo;

public class RemoteMessageCallback extends MessageCallback {
	private ConnectorClient connectorClient;

	public RemoteMessageCallback(String name, ConnectorProducer connectorProducer) {
		super(name);
		this.connectorClient = connectorClient;
	}

	@Override
	public void sendRealtimeMessage(Message message) {
		connectorClient(message);
		logInfo("MessageCallback", "send message to connector: " + message.toString());
	}
}
