package org.eclipse.iofog.local_api;

import org.eclipse.iofog.connector_client.ConnectorProducer;
import org.eclipse.iofog.message_bus.Message;

import static org.eclipse.iofog.utils.logging.LoggingService.logInfo;

public class RemoteMessageCallback extends MessageCallback {
	private ConnectorProducer connectorProducer;

	public RemoteMessageCallback(String name, ConnectorProducer connectorProducer) {
		super(name);
		this.connectorProducer = connectorProducer;
	}

	@Override
	public void sendRealtimeMessage(Message message) {
		connectorProducer.sendMessage(message);
		logInfo("MessageCallback", "send message to connector: " + message.toString());
	}
}
