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
package org.eclipse.iofog.message_bus;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.eclipse.iofog.local_api.MessageCallback;
import org.eclipse.iofog.microservice.Receiver;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.iofog.message_bus.MessageBusServer.messageBusSessionLock;
import static org.eclipse.iofog.utils.logging.LoggingService.logError;

/**
 * Local Message Receiver
 * @author epankou
 */
public class LocalMessageReceiver extends MessageReceiver {
    private static final String MODULE_NAME = "Local Message Receiver";

    LocalMessageReceiver(Receiver receiver, ClientConsumer consumer) {
        super(receiver, consumer);
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    /**
     * enables real-time receiving for this {@link org.eclipse.iofog.microservice.Microservice}
     */
    void enableRealTimeReceiving() {
        if (consumer == null || consumer.isClosed())
            return;
        listener = new MessageListener(new MessageCallback(receiver.getMicroserviceUuid()));
        try {
            consumer.setMessageHandler(listener);
        } catch (ActiveMQException e) {
            logError(MODULE_NAME, "Unable to set message bus handler: " + e.getMessage(), e);
        }
    }

    /**
     * disables real-time receiving for this {@link org.eclipse.iofog.microservice.Microservice}
     */
    void disableRealTimeReceiving() {
        try {
            if (consumer == null || listener == null || consumer.getMessageHandler() == null)
                return;
            listener = null;
            consumer.setMessageHandler(null);
        } catch (Exception exp) {
            logError(MODULE_NAME, exp.getMessage(), exp);
        }
    }

    public synchronized void close() {
        if (consumer == null)
            return;
        disableRealTimeReceiving();
        try {
            consumer.close();
        } catch (Exception exp) {
            logError(MODULE_NAME, exp.getMessage(), exp);
        }
    }

    @Override
    public void update(Receiver receiver) {}

    /**
     * receivers list of {@link Message} sent to this {@link org.eclipse.iofog.microservice.Microservice}
     *
     * @return list of {@link Message}
     * @throws Exception exception
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
     * @throws Exception exception
     */
    private Message getMessage() throws Exception {
        if (consumer == null || listener != null)
            return null;

        Message result = null;
        ClientMessage msg;
        synchronized (messageBusSessionLock) {
            msg = consumer.receiveImmediate();
        }
        if (msg != null) {
            msg.acknowledge();
            result = new Message(msg.getBytesProperty("message"));
        }
        return result;
    }
}
