/*
 * *******************************************************************************
 *  * Copyright (c) 2019 Edgeworx, Inc.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */
package org.eclipse.iofog.connector_client;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.eclipse.iofog.message_bus.Message;
import org.eclipse.iofog.utils.Constants;

import static org.eclipse.iofog.utils.logging.LoggingService.logWarning;

/**
 * IoFog Connector client producer
 * @author epankou
 */
public class ConnectorProducer {
    public final static String MODULE_NAME = "Connector Producer";
    private ClientProducer producer;
    private ClientConfig config;
    private String name;
    private ClientSession session;

    ConnectorProducer(String name, ClientSession session, ClientConfig connectorProducerConfig) {
        this.name = name;
        this.config = connectorProducerConfig;
        try {
            init(session);
        } catch (ActiveMQException e) {
            logWarning(MODULE_NAME, String.format("Connector producer %s creation error: %s", name, e.getMessage()));
        }
    }

    /**
     * Creates activemq producer
     * @param session activemq session
     * @throws ActiveMQException exception if producer creation is unsuccessful
     */
    void init(ClientSession session) throws ActiveMQException {
        this.session = session;
        this.producer = create(session);
    }

    private ClientProducer create(ClientSession session) throws ActiveMQException {
        ClientProducer producer = null;
        if (session != null) {
            producer = session.createProducer(Constants.ACTIVEMQ_ADDRESS);
        }
        return producer;
    }

    public synchronized ClientConfig getConfig() {
        return config;
    }

    public String getName() {
        return name;
    }

    public synchronized void sendMessage(Message message) {
        if (!producer.isClosed()) {
            ClientMessage msg = session.createMessage(false);
            byte[] bytesMsg = message.getBytes();
            msg.putStringProperty("key", config.getPassKey());
            msg.putBytesProperty("message", bytesMsg);

            try {
                producer.send(msg);
            } catch (ActiveMQException e) {
                logWarning(MODULE_NAME, "Message sending error: " + e.getMessage());
            }
        }
    }

    public synchronized void close() {
        if (producer != null && !producer.isClosed()) {
            try {
                producer.close();
            } catch (ActiveMQException e) {
                logWarning(MODULE_NAME, String.format("Unable to close connector producer %s: %s", name, e.getMessage()));
            }
        }
    }

    public synchronized boolean isClosed() {
        return producer == null || producer.isClosed();
    }
}
