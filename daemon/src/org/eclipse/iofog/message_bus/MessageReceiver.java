/*******************************************************************************
 * Copyright (c) 2018 Edgeworx, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Saeid Baghbidi
 * Kilton Hopkins
 *  Ashita Nagar
 *******************************************************************************/
package org.eclipse.iofog.message_bus;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.eclipse.iofog.connector_client.ConnectorManager;
import org.eclipse.iofog.connector_client.ConnectorProducer;
import org.eclipse.iofog.local_api.MessageCallback;
import org.eclipse.iofog.local_api.RemoteMessageCallback;
import org.eclipse.iofog.microservice.Microservice;
import org.eclipse.iofog.microservice.Receiver;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.iofog.utils.logging.LoggingService.logWarning;

/**
 * receiver {@link Microservice}
 * 
 * @author saeid
 *
 */
public class MessageReceiver implements AutoCloseable {
	private static final String MODULE_NAME = "Message Receiver";

	private Receiver receiver;
	private MessageListener listener;
	private final ClientConsumer consumer;
	private ConnectorProducer connectorProducer;

	MessageReceiver(Receiver receiver, ClientConsumer consumer) {
		this.receiver = receiver;
		this.consumer = consumer;
		enableConnectorRealTimeProducing();
	}

	public synchronized Receiver getReceiver() {
		return receiver;
	}

	synchronized ConnectorProducer getConnectorProducer() {
		return connectorProducer;
	}

	synchronized void updateReceiver(Receiver receiver) {
		if (!this.receiver.equals(receiver)) {
			if (this.receiver.isLocal() != receiver.isLocal()) {
				if (!receiver.isLocal()) {
					this.receiver = receiver;
					enableConnectorRealTimeProducing();
				} else {
					disableConnectorRealTimeProducing();
					this.receiver = receiver;
				}
			} else if (!this.receiver.isLocal()
				&& !this.receiver.getConnectorProducerConfig().equals(receiver.getConnectorProducerConfig())) {
				disableConnectorRealTimeProducing();
				this.receiver = receiver;
				enableConnectorRealTimeProducing();
			} else {
				this.receiver = receiver;
			}
		}
	}

	/**
	 * receivers list of {@link Message} sent to this {@link Microservice}
	 * 
	 * @return list of {@link Message}
	 * @throws Exception
	 */
	synchronized List<Message> getMessages() throws Exception {
		List<Message> result = new ArrayList<>();
		
		if (consumer != null || listener == null) {
			Message message = getMessage();
			while (message != null) {
				result.add(message);
				message = getMessage();
			}
		}
		return result;
	}

	/**
	 * receives only one {@link Message}
	 * 
	 * @return {@link Message}
	 * @throws Exception
	 */
	private Message getMessage() throws Exception {
		if (consumer == null || listener != null)
			return null;

		Message result = null; 
		ClientMessage msg = consumer.receiveImmediate();
		if (msg != null) {
			msg.acknowledge();
			result = new Message(msg.getBytesProperty("message"));
		}
		return result;
	}

	synchronized void enableConnectorRealTimeProducing() {
		if (!receiver.isLocal() && consumer != null && !consumer.isClosed()) {
			connectorProducer = ConnectorManager.INSTANCE.getConnectorProducer(
				receiver.getMicroserviceUuid(),
				receiver.getConnectorProducerConfig()
			);
			if (connectorProducer != null && !connectorProducer.isClosed()) {
				listener = new MessageListener(new RemoteMessageCallback(
					receiver.getMicroserviceUuid(),
					connectorProducer)
				);
				try {
					consumer.setMessageHandler(listener);
				} catch (ActiveMQException e) {
					logWarning(MODULE_NAME, "Unable to set message bus handler: " + e.getMessage());
				}
			}
		}
	}

	private void disableConnectorRealTimeProducing() {
		if (!receiver.isLocal() && connectorProducer != null) {
			connectorProducer.closeProducer();
		}
	}
	
	/**
	 * enables real-time receiving for this {@link Microservice}
	 * 
	 */
	void enableRealTimeReceiving() {
		if (consumer == null || consumer.isClosed())
			return;
		listener = new MessageListener(new MessageCallback(receiver.getMicroserviceUuid()));
		try {
			consumer.setMessageHandler(listener);
		} catch (ActiveMQException e) {
			logWarning(MODULE_NAME, "Unable to set message bus handler: " + e.getMessage());
		}
	}
	
	/**
	 * disables real-time receiving for this {@link Microservice}
	 * 
	 */
	void disableRealTimeReceiving() {
		try {
			if (consumer == null || listener == null || consumer.getMessageHandler() == null)
				return;
			listener = null;
			consumer.setMessageHandler(null);
		} catch (Exception exp) {
			logWarning(MODULE_NAME, exp.getMessage());
		}
	}
	
	public synchronized void close() {
		if (consumer == null)
			return;
		disableRealTimeReceiving();
		disableConnectorRealTimeProducing();
		try {
			consumer.close();
		} catch (Exception exp) {
			logWarning(MODULE_NAME, exp.getMessage());
		}
	}
}
