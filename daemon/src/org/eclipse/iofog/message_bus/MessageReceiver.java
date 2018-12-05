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

import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.eclipse.iofog.connector_client.ConnectorManager;
import org.eclipse.iofog.connector_client.ConnectorProducer;
import org.eclipse.iofog.connector_client.ConnectorProducerConfig;
import org.eclipse.iofog.local_api.MessageCallback;
import org.eclipse.iofog.local_api.RemoteMessageCallback;
import org.eclipse.iofog.microservice.Microservice;

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

	private final String name;
	private boolean isLocal;
	private ConnectorProducerConfig connectorProducerConfig;
	private MessageListener listener;
	private final ClientConsumer consumer;
	private ConnectorProducer connectorProducer;

	public MessageReceiver(String name, boolean isLocal, ConnectorProducerConfig connectorProducerConfig, ClientConsumer consumer) {
		this.name = name;
		this.isLocal = isLocal;
		this.connectorProducerConfig = connectorProducerConfig;
		this.consumer = consumer;
		enableConnectorRealTimeProducing();
	}

	public boolean isLocal() {
		return isLocal;
	}

	public ConnectorProducerConfig getConnectorProducerConfig() {
		return connectorProducerConfig;
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

	protected String getName() {
		return name;
	}

	private void enableConnectorRealTimeProducing() {
		if (!isLocal) {
			if (consumer == null || consumer.isClosed())
				return;

			connectorProducer = ConnectorManager.INSTANCE.getConnectorProducer(name, connectorProducerConfig);
			if (connectorProducer != null) {
				listener = new MessageListener(new RemoteMessageCallback(name, connectorProducer));
				try {
					consumer.setMessageHandler(listener);
				} catch (Exception e) {
					listener = null;
				}
			}
		}
	}

	private void disableConnectorRealTimeProducing() {
		if (!isLocal && connectorProducer != null) {
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
		listener = new MessageListener(new MessageCallback(name));
		try {
			consumer.setMessageHandler(listener);
		} catch (Exception e) {
			listener = null;
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
	
	public void close() {
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
