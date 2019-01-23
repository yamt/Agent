package org.eclipse.iofog.connector_client;

public class ClientConfig {
    private int connectorId;
    private String publisherId;
    private String passKey;

    public ClientConfig(int connectorId, String publisherId, String passKey) {
        this.connectorId = connectorId;
        this.publisherId = publisherId;
        this.passKey = passKey;
    }

    int getConnectorId() {
        return connectorId;
    }

    String getPublisherId() {
        return publisherId;
    }

    String getPassKey() {
        return passKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClientConfig that = (ClientConfig) o;

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
