package org.eclipse.iofog.connector_client;

public class ConnectorConfig {
	private String host;
	private int port;
	private String user;
	private String password;
	private String passKey;

	public ConnectorConfig(String host, int port, String user, String password, String passKey) {
		this.host = host;
		this.port = port;
		this.user = user;
		this.password = password;
		this.passKey = passKey;
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

	public String getPassKey() {
		return passKey;
	}

	public void setPassKey(String passKey) {
		this.passKey = passKey;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ConnectorConfig that = (ConnectorConfig) o;

		if (port != that.port) return false;
		if (!host.equals(that.host)) return false;
		if (!user.equals(that.user)) return false;
		if (!password.equals(that.password)) return false;
		return passKey.equals(that.passKey);
	}

	@Override
	public int hashCode() {
		int result = host.hashCode();
		result = 31 * result + port;
		result = 31 * result + user.hashCode();
		result = 31 * result + password.hashCode();
		result = 31 * result + passKey.hashCode();
		return result;
	}
}
