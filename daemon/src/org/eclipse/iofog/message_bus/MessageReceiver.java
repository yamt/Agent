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
import org.eclipse.iofog.connector_client.ConnectorClient;
import org.eclipse.iofog.connector_client.ConnectorClientOld;
import org.eclipse.iofog.connector_client.ConnectorManager;
import org.eclipse.iofog.local_api.MessageCallback;
import org.eclipse.iofog.local_api.RemoteMessageCallback;
import org.eclipse.iofog.microservice.Microservice;
import org.eclipse.iofog.connector_client.ConnectorConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.eclipse.iofog.utils.logging.LoggingService.logInfo;
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
	private Integer connectorId;
	private MessageListener listener;
	private final ClientConsumer consumer;
	private ConnectorClient connectorClient;
	private CompletableFuture<Void> connectorFuture;

	public MessageReceiver(String name, boolean isLocal, Integer connectorId, ClientConsumer consumer) {
		this.name = name;
		this.isLocal = isLocal;
		this.connectorId = connectorId;
		this.consumer = consumer;
		enableConnectorRealTimeProducing(connectorId);
	}

	public boolean isLocal() {
		return isLocal;
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

	private void enableConnectorRealTimeProducing(Integer connectorId) {
		if (!isLocal) {
			if (consumer == null || consumer.isClosed())
				return;

			connectorFuture = ConnectorManager.INSTANCE.createConnectorProducer(name, connectorId)
				.thenApplyAsync((connectorProducer) -> {
					listener = new MessageListener(new RemoteMessageCallback(name, ConnectorManager.INSTANCE));
					try {
						consumer.setMessageHandler(listener);
					} catch (Exception e) {
						listener = null;
					}
				});
		}
	}

	private void disableConnectorRealTimeProducing() {
		if (!isLocal) {
			if (!connectorFuture.isDone()) {
				connectorFuture.cancel(true);
			}
			connectorClient.closeProducer();
		}
	}

	private void createConnectorSession(ConnectorConfig routeConfig) {
		boolean isConnectorSessionCreated = false;
		while(!isConnectorSessionCreated) {
			if (Thread.currentThread().isInterrupted()) {
				break;
			}
			try {
				this.connectorClient = new ConnectorClientOld(routeConfig);
				isConnectorSessionCreated = true;
			} catch (Exception e) {
				logWarning(MODULE_NAME, "Unable to create connector session: " + e.getMessage());
				logInfo(MODULE_NAME, "Going to create connector session in 10 seconds.");
				try {
					Thread.sleep(10000);
				} catch (InterruptedException ex) {
					logInfo(MODULE_NAME, ex.getMessage());
				}
			}
		}
	}

	private void createConnectorProducer() {
		boolean isProducerCreated = false;
		while(!isProducerCreated) {
			if (Thread.currentThread().isInterrupted()) {
				break;
			}
			try {
				connectorClient.createProducer();
				isProducerCreated = true;
				logInfo(MODULE_NAME, "Connector producer has been created.");
			} catch (ActiveMQException e) {
				logWarning(MODULE_NAME, "Failed to create connector producer: " + e.getMessage());
				logInfo(MODULE_NAME, "Going to create connector producer in 10 seconds.");
				try {
					Thread.sleep(10000);
				} catch (InterruptedException ex) {
					logInfo(MODULE_NAME, ex.getMessage());
				}
			}
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
