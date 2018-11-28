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
package org.eclipse.iofog.microservice;

import java.util.List;

/**
 * represents microservice routings
 * 
 * @author saeid
 *
 */
public class Route {
	private Producer producer;
	private List<Receiver> receivers;

	public Route(Producer producer, List<Receiver> receivers) {
		this.producer = producer;
		this.receivers = receivers;
	}

	public Producer getProducer() {
		return producer;
	}

	public void setProducer(Producer producer) {
		this.producer = producer;
	}

	public List<Receiver> getReceivers() {
		return receivers;
	}

	public void setReceivers(List<Receiver> receivers) {
		this.receivers = receivers;
	}

	@Override
	public String toString() {
		StringBuilder in = new StringBuilder("\"producer\" : ")
				.append(producer.getMicroserviceId())
				.append(", \"receivers\" : [");
		if (receivers != null)
			for (Receiver receiver : receivers)
				in.append("\"").append(receiver.getMicroserviceUuid()).append("\",");
		in.append("]");
		return "{" + in + "}";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Route route = (Route) o;

		if (!producer.equals(route.producer)) return false;
		return receivers.equals(route.receivers);
	}

	@Override
	public int hashCode() {
		int result = producer.hashCode();
		result = 31 * result + receivers.hashCode();
		return result;
	}
}
