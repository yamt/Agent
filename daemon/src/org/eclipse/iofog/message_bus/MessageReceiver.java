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
