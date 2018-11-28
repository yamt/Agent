package org.eclipse.iofog.connector_client;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.*;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.eclipse.iofog.message_bus.Message;
import org.eclipse.iofog.microservice.RouteConfig;

import java.util.HashMap;
import java.util.Map;

import static org.eclipse.iofog.utils.logging.LoggingService.logWarning;

public class ConnectorClient {

	public final static String MODULE_NAME = "Connector Client";

	private RouteConfig routeConfig;
	private ClientSession session;
	private ClientProducer producer;
	private ClientConsumer consumer;

	public ConnectorClient(RouteConfig routeConfig) {
		this.routeConfig = routeConfig;
		createSession();
	}

	private void createSession() {
		Map<String, Object> connectionParams = new HashMap<>();
		connectionParams.put(TransportConstants.PORT_PROP_NAME, routeConfig.getPort());
		connectionParams.put(TransportConstants.HOST_PROP_NAME, routeConfig.getHost());

		TransportConfiguration transportConfiguration =
				new TransportConfiguration(
						"org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory",
						connectionParams);

		ServerLocator locator = ActiveMQClient.createServerLocatorWithoutHA(transportConfiguration);
		try {
			ClientSessionFactory factory = locator.createSessionFactory();
			this.session = factory.createSession(routeConfig.getUser(), routeConfig.getPassword(), false, true, true, false, 0);
		} catch (ActiveMQException ex) {
			logWarning(MODULE_NAME, "Unable to open connector session: " + ex.getMessage());
		} catch (Exception ex) {
			logWarning(MODULE_NAME, "Unable to create connector session factory: " + ex.getMessage());
		}
	}

	public void createProducer() {
		try {
			this.producer = session.createProducer("pubsub.iofog");
			startSession();
		} catch (ActiveMQException e) {
			logWarning(MODULE_NAME, "Unable to create connector producer: " + e.getMessage());
		}
	}

	private void createConsumer() {
		try {
			this.consumer = session.createConsumer("pubsub.iofog");
			startSession();
		} catch (ActiveMQException e) {
			logWarning(MODULE_NAME, "Unable to create connector consumer: " + e.getMessage());
		}
	}

	private void startSession() {
		if (session.isClosed()) {
			try {
				session.start();
			} catch (ActiveMQException e) {
				logWarning(MODULE_NAME, "Unable to start connector session: " + e.getMessage());
			}
		}
	}

	public void closeProducer() {
		if (producer != null && !producer.isClosed()) {
			try {
				producer.close();
				closeSession();
			} catch (ActiveMQException e) {
				logWarning(MODULE_NAME, "Unable to close connector producer: " + e.getMessage());
			}
		}
	}

	public void closeConsumer() {
		if (consumer != null && !consumer.isClosed()) {
			try {
				consumer.close();
				closeSession();
			} catch (ActiveMQException e) {
				logWarning(MODULE_NAME, "Unable to close connector consumer: " + e.getMessage());
			}
		}
	}

	private void closeSession() {
		if (!session.isClosed()) {
			try {
				session.close();
			} catch (ActiveMQException e) {
				logWarning(MODULE_NAME, "Unable to close connector session: " + e.getMessage());
			}
		}

	}

	public void sendRealTimeMessage(Message message) {
		ClientMessage clientMessage = session.createMessage(true);
		byte[] bytesMsg = message.getBytes();
		clientMessage.getBodyBuffer().writeBytes(bytesMsg);
		clientMessage.putStringProperty("token", routeConfig.getPassKey());
		if (producer != null) {
			try {
				producer.send(clientMessage);
			} catch (ActiveMQException e) {
				logWarning(MODULE_NAME, "Message sending error: " + e.getMessage());
			}
		} else {
			logWarning(MODULE_NAME, "Producer has not been created");
		}

	}

	public void setMessageListener(MessageHandler handler) {
		createConsumer();
		try {
			consumer.setMessageHandler(handler);
		} catch (ActiveMQException e) {
			logWarning(MODULE_NAME, "Unable to set connector message handler: " + e.getMessage());
		}
	}
}
