package org.eclipse.iofog.connector_client;

public class ConnectorConfig {
	private String name;
	private String host;
	private int port;
	private String user;
	private String password;
	private boolean isDevModeEnabled;
	private String cert;
	private boolean isSelfSignedCert;
	private String keystorePassword;

	public ConnectorConfig(String name, String host, int port, String user, String password, boolean isDevModeEnabled,
						   String cert, boolean isSelfSignedCert, String keystorePassword) {
		this.name = name;
		this.host = host;
		this.port = port;
		this.user = user;
		this.password = password;
		this.isDevModeEnabled = isDevModeEnabled;
		this.cert = cert;
		this.isSelfSignedCert = isSelfSignedCert;
		this.keystorePassword = keystorePassword;
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

	public String getCert() {
		return cert;
	}

	public boolean isSelfSignedCert() {
		return isSelfSignedCert;
	}

	public String getKeystorePassword() {
		return keystorePassword;
	}

	public String getName() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ConnectorConfig that = (ConnectorConfig) o;

		if (port != that.port) return false;
		if (isDevModeEnabled != that.isDevModeEnabled) return false;
		if (isSelfSignedCert != that.isSelfSignedCert) return false;
		if (!name.equals(that.name)) return false;
		if (!host.equals(that.host)) return false;
		if (!user.equals(that.user)) return false;
		if (!password.equals(that.password)) return false;
		if (cert != null ? !cert.equals(that.cert) : that.cert != null) return false;
		return keystorePassword != null ? keystorePassword.equals(that.keystorePassword) : that.keystorePassword == null;
	}

	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + host.hashCode();
		result = 31 * result + port;
		result = 31 * result + user.hashCode();
		result = 31 * result + password.hashCode();
		result = 31 * result + (isDevModeEnabled ? 1 : 0);
		result = 31 * result + (cert != null ? cert.hashCode() : 0);
		result = 31 * result + (isSelfSignedCert ? 1 : 0);
		result = 31 * result + (keystorePassword != null ? keystorePassword.hashCode() : 0);
		return result;
	}
}
