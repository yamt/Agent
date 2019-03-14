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
package org.eclipse.iofog.local_api;

import org.eclipse.iofog.connector_client.ConnectorProducer;
import org.eclipse.iofog.message_bus.Message;

/**
 * Message callback to handle messages for IoFog Connector
 * @author epankou
 */
public class RemoteMessageCallback extends MessageCallback {
	private ConnectorProducer connectorProducer;

	public RemoteMessageCallback(String name, ConnectorProducer connectorProducer) {
		super(name);
		this.connectorProducer = connectorProducer;
	}

	@Override
	public void sendRealtimeMessage(Message message) {
		connectorProducer.sendMessage(message);
	}
}
