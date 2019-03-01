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
import org.eclipse.iofog.microservice.Microservice;
import org.eclipse.iofog.microservice.Receiver;

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


}
