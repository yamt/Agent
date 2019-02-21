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
import org.eclipse.iofog.connector_client.ConnectorConsumer;
import org.eclipse.iofog.connector_client.ConnectorManager;
import org.eclipse.iofog.microservice.Microservice;
import org.eclipse.iofog.microservice.Receiver;
import org.eclipse.iofog.microservice.Route;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.eclipse.iofog.message_bus.MessageBus.MODULE_NAME;
import static org.eclipse.iofog.message_bus.MessageBusServer.messageBusSessionLock;
import static org.eclipse.iofog.utils.logging.LoggingService.logError;

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
	private ConnectorConsumer connectorConsumer;

	MessagePublisher(Route route, ClientProducer producer) {
		this.archive = new MessageArchive(route.getProducer().getMicroserviceId());
		this.route = route;
		this.producer = producer;
		this.session = MessageBusServer.getSession();
		enableConnectorConsuming();
	}

	ConnectorConsumer getConnectorConsumer() {
		return connectorConsumer;
	}

	synchronized Route getRoute() {
		return route;
	}

	synchronized void enableConnectorConsuming() {
		if (!route.getProducer().isLocal() && producer != null && !producer.isClosed()) {
			String name = route.getProducer().getMicroserviceId();
			connectorConsumer = ConnectorManager.INSTANCE.getConsumer(
				name,
				route.getProducer().getConnectorConsumerConfig()
			);
		}
	}

	private void disableConnectorConsuming() {
		if (!route.getProducer().isLocal() && connectorConsumer != null) {
			ConnectorManager.INSTANCE.removeConsumer(connectorConsumer.getName());
		}
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
			LoggingService.logError(
				"Message Publisher (" + this.route.getProducer().getMicroserviceId() + ")",
				"unable to archive massage --> " + e.getMessage(),
                e
			);
		}

		for (Receiver receiver : getFilteredReceivers()) {
			String name = receiver.getMicroserviceUuid();
			ClientMessage msg = session.createMessage(false);
			msg.putObjectProperty("receiver", name);
			msg.putBytesProperty("message", bytes);
			synchronized (messageBusSessionLock) {
				producer.send(msg);
			}
		}
	}

	private List<Receiver> getFilteredReceivers() {
		List<Receiver> receivers = route.getReceivers();
		Map<Integer, List<Receiver>> receiverMap = receivers.stream()
			.filter(receiver -> !receiver.isLocal())
			.collect(groupingBy(receiver -> receiver.getConnectorProducerConfig().getConnectorId()));

		List<Receiver> remoteReceivers = receiverMap.values().stream()
			.map(Collections::min)
			.collect(toList());

		return Stream.concat(
			receivers.stream()
				.filter(Receiver::isLocal),
			remoteReceivers.stream()
		).collect(toList());
	}

	synchronized void updateRoute(Route route) {
		if (!this.route.equals(route)) {
			if (this.route.getProducer().isLocal() != route.getProducer().isLocal()) {
				if (!route.getProducer().isLocal()) {
					this.route = route;
					enableConnectorConsuming();
				} else {
					disableConnectorConsuming();
					this.route = route;
				}
			} else if (!this.route.getProducer().isLocal()
				&& !this.route.getProducer().getConnectorConsumerConfig().equals(route.getProducer().getConnectorConsumerConfig())) {
				disableConnectorConsuming();
				this.route = route;
				enableConnectorConsuming();
 			} else {
				this.route = route;
			}
		}
	}

	public synchronized void close() {
		try {
			archive.close();
			disableConnectorConsuming();
		} catch (Exception exp) {
			logError(MODULE_NAME, exp.getMessage(), exp);
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
