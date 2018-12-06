package org.eclipse.iofog.connector_client;

public class ConnectorProducerConfig {
    private Integer connectorId;
    private String topicName;
    private String passKey;

    public ConnectorProducerConfig(Integer connectorId, String topicName, String passKey) {
        this.connectorId = connectorId;
        this.topicName = topicName;
        this.passKey = passKey;
    }

    public Integer getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(Integer connectorId) {
        this.connectorId = connectorId;
    }

    public String getPassKey() {
        return passKey;
    }

    public void setPassKey(String passKey) {
        this.passKey = passKey;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConnectorProducerConfig that = (ConnectorProducerConfig) o;

        if (!connectorId.equals(that.connectorId)) return false;
        if (!topicName.equals(that.topicName)) return false;
        return passKey.equals(that.passKey);
    }

    @Override
    public int hashCode() {
        int result = connectorId.hashCode();
        result = 31 * result + topicName.hashCode();
        result = 31 * result + passKey.hashCode();
        return result;
    }
}
