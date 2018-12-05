package org.eclipse.iofog.connector_client;

public class ConnectorConsumerConfig {
    private Integer connectorId;
    private String topicName;
    private String passKey;

    public ConnectorConsumerConfig(Integer connectorId, String topicName, String filter) {
        this.connectorId = connectorId;
        this.topicName = topicName;
        this.passKey = filter;
    }

    public Integer getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(Integer connectorId) {
        this.connectorId = connectorId;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
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

        ConnectorConsumerConfig that = (ConnectorConsumerConfig) o;

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
