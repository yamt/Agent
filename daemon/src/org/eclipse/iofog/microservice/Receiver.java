package org.eclipse.iofog.microservice;

import org.eclipse.iofog.connector_client.ConnectorProducerConfig;

public class Receiver {
	private String microserviceUuid;
	private boolean isLocal;
	private ConnectorProducerConfig connectorProducerConfig;

	public Receiver(String microserviceUuid, boolean isLocal, ConnectorProducerConfig connectorProducerConfig) {
		this.microserviceUuid = microserviceUuid;
		this.isLocal = isLocal;
		this.connectorProducerConfig = connectorProducerConfig;
	}

	public String getMicroserviceUuid() {
		return microserviceUuid;
	}

	public void setMicroserviceUuid(String microserviceUuid) {
		this.microserviceUuid = microserviceUuid;
	}

	public boolean isLocal() {
		return isLocal;
	}

	public void setLocal(boolean local) {
		isLocal = local;
	}

	public ConnectorProducerConfig getConnectorProducerConfig() {
		return connectorProducerConfig;
	}

	public void setConnectorProducerConfig(ConnectorProducerConfig connectorProducerConfig) {
		this.connectorProducerConfig = connectorProducerConfig;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Receiver receiver = (Receiver) o;

		if (isLocal != receiver.isLocal) return false;
		if (!microserviceUuid.equals(receiver.microserviceUuid)) return false;
		return connectorProducerConfig != null ? connectorProducerConfig.equals(receiver.connectorProducerConfig) : receiver.connectorProducerConfig == null;
	}

	@Override
	public int hashCode() {
		int result = microserviceUuid.hashCode();
		result = 31 * result + (isLocal ? 1 : 0);
		result = 31 * result + (connectorProducerConfig != null ? connectorProducerConfig.hashCode() : 0);
		return result;
	}
}
