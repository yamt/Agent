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

import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.eclipse.iofog.connector_client.ConnectorClient;
import org.eclipse.iofog.microservice.Microservice;
import org.eclipse.iofog.microservice.Receiver;
import org.eclipse.iofog.microservice.Route;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.util.List;

import static org.eclipse.iofog.message_bus.MessageBus.MODULE_NAME;
import static org.eclipse.iofog.utils.logging.LoggingService.logWarning;

/**
 * publisher {@link Microservice}
 * 
 * @author saeid
 *
 */
public class MessagePublisher implements AutoCloseable {
	private final MessageArchive archive;
	private Route route;
	private ClientProducer producer;
	private ClientSession session;
	private ConnectorClient connectorClient;

	public MessagePublisher(Route route, ClientProducer producer) {
		this.archive = new MessageArchive(route.getProducer().getMicroserviceId());
		this.route = route;
		this.producer = producer;
		this.session = MessageBusServer.getSession();
		enableConnectorRealTimeReceiving();
	}

	public Route getRoute() {
		return route;
	}

	public void setRoute(Route route) {
		this.route = route;
	}

	private void enableConnectorRealTimeReceiving() {
//		if (!route.getProducer().isLocal() && producer != null && !producer.isClosed()) {
//			ConnectorMessageListener listener = new ConnectorMessageListener(new ConnectorMessageCallback());
//			connectorClient = new ConnectorClient(route.getProducer().getRouteConfig());
//			connectorClient.setMessageListener(listener);
//		}

		if (route.getProducer().isLocal()) {

			for (Receiver receiver : route.getReceivers()) {
				if (!receiver.isLocal()) {

				}
			}
		}
	}

	private void disableConnectorRealTimeReceiving() {
		connectorClient.closeConsumer();
	}

	/**
	 * publishes a {@link Message}
	 * 
	 * @param message - {@link Message} to be published
	 * @throws Exception
	 */
	synchronized void publish(Message message) throws Exception {
		byte[] bytes = message.getBytes();

		try {
			archive.save(bytes, message.getTimestamp());
		} catch (Exception e) {
			LoggingService.logWarning(
				"Message Publisher (" + this.route.getProducer().getMicroserviceId() + ")",
				"unable to archive massage --> " + e.getMessage()
			);
		}
		for (Receiver receiver : route.getReceivers()) {
			String name = receiver.getMicroserviceUuid();
			ClientMessage msg = session.createMessage(false);
			msg.putObjectProperty("receiver", name);
			msg.putBytesProperty("message", bytes);
			producer.send(msg);
		}
	}

	synchronized void updateRoute(Route route) {
		if (!this.route.equals(route)) {
			if (this.route.getProducer().isLocal() != route.getProducer().isLocal()) {
				if (this.route.getProducer().isLocal()) {
					this.route = route;
					enableConnectorRealTimeReceiving();
				} else {
					disableConnectorRealTimeReceiving();
					this.route = route;
				}
			}
		} else if (!this.route.getProducer().isLocal()
			&& !this.route.getProducer().getRouteConfig().equals(route.getProducer().getRouteConfig())) {
			disableConnectorRealTimeReceiving();
			this.route = route;
			enableConnectorRealTimeReceiving();
		} else {
			this.route = route;
		}
	}

	public synchronized void close() {
		try {
			archive.close();
		} catch (Exception exp) {
			logWarning(MODULE_NAME, exp.getMessage());
		}
	}

	/**
	 * retrieves list of {@link Message} published by this {@link Microservice}
	 * within a time frame
	 * 
	 * @param from - beginning of time frame
	 * @param to - end of time frame
	 * @return list of {@link Message}
	 */
	public synchronized List<Message> messageQuery(long from, long to) {
		return archive.messageQuery(from, to);
	}
	
}
