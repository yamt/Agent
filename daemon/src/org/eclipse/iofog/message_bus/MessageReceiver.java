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
import org.eclipse.iofog.microservice.Microservice;
import org.eclipse.iofog.microservice.Receiver;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.iofog.message_bus.MessageBusServer.messageBusSessionLock;

/**
 * receiver {@link Microservice}
 *
 * @author saeid
 */
public abstract class MessageReceiver implements AutoCloseable {
    MessageListener listener;
    ClientConsumer consumer;
    Receiver receiver;

    MessageReceiver(Receiver receiver, ClientConsumer consumer) {
        this.consumer = consumer;
        this.receiver = receiver;
    }

    public abstract boolean isLocal();

    public abstract void close();

    public abstract void update(Receiver receiver);

    public synchronized Receiver getReceiver() {
        return receiver;
    }

    /**
     * receivers list of {@link Message} sent to this {@link Microservice}
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
