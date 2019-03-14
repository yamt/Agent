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

import org.eclipse.iofog.message_bus.Message;
import org.eclipse.iofog.message_bus.MessageBusUtil;

/**
 * IoFog Connector message callback
 * @author epankou
 */
class ConnectorMessageCallback {
    /**
     * Sends IoFog Connector message to Message Bus
     * @param message IoMessage
     */
    void sendConnectorMessage(Message message) {
        MessageBusUtil messageBus = new MessageBusUtil();
        messageBus.publishMessage(message);
    }
}
