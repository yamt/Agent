package org.eclipse.iofog.connector_client;

public class ConnectorClientConfig {
    private int connectorId;
    private String publisherId;
    private String passKey;

    public ConnectorClientConfig(int connectorId, String publisherId, String passKey) {
        this.connectorId = connectorId;
        this.publisherId = publisherId;
        this.passKey = passKey;
    }

    public int getConnectorId() {
        return connectorId;
    }

    public String getPublisherId() {
        return publisherId;
    }

    public String getPassKey() {
        return passKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConnectorClientConfig that = (ConnectorClientConfig) o;

        if (connectorId != that.connectorId) return false;
        if (!publisherId.equals(that.publisherId)) return false;
        return passKey.equals(that.passKey);
    }

    @Override
    public int hashCode() {
        int result = connectorId;
        result = 31 * result + publisherId.hashCode();
        result = 31 * result + passKey.hashCode();
        return result;
    }
}
