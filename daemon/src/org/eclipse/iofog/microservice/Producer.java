package org.eclipse.iofog.microservice;

public class Producer {
	private String microserviceId;
	private boolean isLocal;
	private RouteConfig routeConfig;

	public Producer(String microserviceId, boolean isLocal, RouteConfig routeConfig) {
		this.microserviceId = microserviceId;
		this.isLocal = isLocal;
		this.routeConfig = routeConfig;
	}

	public String getMicroserviceId() {
		return microserviceId;
	}

	public void setMicroserviceId(String microserviceId) {
		this.microserviceId = microserviceId;
	}

	public boolean isLocal() {
		return isLocal;
	}

	public void setLocal(boolean local) {
		isLocal = local;
	}

	public RouteConfig getRouteConfig() {
		return routeConfig;
	}

	public void setRouteConfig(RouteConfig routeConfig) {
		this.routeConfig = routeConfig;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Producer producer = (Producer) o;

		if (isLocal != producer.isLocal) return false;
		if (!microserviceId.equals(producer.microserviceId)) return false;
		return routeConfig != null ? routeConfig.equals(producer.routeConfig) : producer.routeConfig == null;
	}

	@Override
	public int hashCode() {
		int result = microserviceId.hashCode();
		result = 31 * result + (isLocal ? 1 : 0);
		result = 31 * result + (routeConfig != null ? routeConfig.hashCode() : 0);
		return result;
	}
}
