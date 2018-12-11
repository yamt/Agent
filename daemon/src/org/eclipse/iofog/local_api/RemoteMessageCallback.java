package org.eclipse.iofog.local_api;

import org.eclipse.iofog.connector_client.ConnectorProducer;
import org.eclipse.iofog.message_bus.Message;

public class RemoteMessageCallback extends MessageCallback {
	private ConnectorProducer connectorProducer;

	public RemoteMessageCallback(String name, ConnectorProducer connectorProducer) {
		super(name);
		this.connectorProducer = connectorProducer;
	}

	@Override
	public void sendRealtimeMessage(Message message) {
		connectorProducer.sendMessage(message);
	}
}
