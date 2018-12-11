package org.eclipse.iofog.connector_client;

public class ConnectorClientConfig {
    private int connectorId;
    private String topicName;
    private String passKey;

    public ConnectorClientConfig(int connectorId, String topicName, String passKey) {
        this.connectorId = connectorId;
        this.topicName = topicName;
        this.passKey = passKey;
    }

    public int getConnectorId() {
        return connectorId;
    }

    public String getTopicName() {
        return topicName;
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
        if (!topicName.equals(that.topicName)) return false;
        return passKey.equals(that.passKey);
    }

    @Override
    public int hashCode() {
        int result = connectorId;
        result = 31 * result + topicName.hashCode();
        result = 31 * result + passKey.hashCode();
        return result;
    }
}
