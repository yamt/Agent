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
package org.eclipse.iofog.local_api;

import org.eclipse.iofog.message_bus.Message;

import static org.eclipse.iofog.utils.logging.LoggingService.logInfo;

/**
 * Interface for the message bus to send real-time messages 
 * @author ashita
 * @since 2016
 */
public class MessageCallback {
	private static final String MODULE_NAME = "Message Callback";
	private final String name;
	
	public MessageCallback(String name) {
		this.name = name;
	}
	
	/**
	 * Method called from message bus to send real-time messages to the containers
	 * @param message
	 * @return void
	 */
	public void sendRealtimeMessage(Message message) {
		MessageWebsocketHandler handler = new MessageWebsocketHandler();
		handler.sendRealTimeMessage(name, message);
		logInfo(MODULE_NAME, "Sent real time message to container");
	}
}