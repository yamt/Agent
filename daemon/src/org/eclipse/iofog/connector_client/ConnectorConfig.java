package org.eclipse.iofog.connector_client;

public class ConnectorConfig {
	private String host;
	private int port;
	private String user;
	private String password;

	public ConnectorConfig(String host, int port, String user, String password) {
		this.host = host;
		this.port = port;
		this.user = user;
		this.password = password;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ConnectorConfig that = (ConnectorConfig) o;

		if (port != that.port) return false;
		if (!host.equals(that.host)) return false;
		if (!user.equals(that.user)) return false;
		return password.equals(that.password);
	}

	@Override
	public int hashCode() {
		int result = host.hashCode();
		result = 31 * result + port;
		result = 31 * result + user.hashCode();
		result = 31 * result + password.hashCode();
		return result;
	}
}
