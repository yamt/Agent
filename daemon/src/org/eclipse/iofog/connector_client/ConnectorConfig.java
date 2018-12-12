package org.eclipse.iofog.connector_client;

public class ConnectorConfig {
	private String host;
	private int port;
	private String user;
	private String password;
	private boolean isDevModeEnabled;

	public ConnectorConfig(String host, int port, String user, String password, boolean isDevModeEnabled) {
		this.host = host;
		this.port = port;
		this.user = user;
		this.password = password;
		this.isDevModeEnabled = isDevModeEnabled;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}

	public boolean isDevModeEnabled() {
		return isDevModeEnabled;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ConnectorConfig that = (ConnectorConfig) o;

		if (port != that.port) return false;
		if (isDevModeEnabled != that.isDevModeEnabled) return false;
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
		result = 31 * result + (isDevModeEnabled ? 1 : 0);
		return result;
	}
}
