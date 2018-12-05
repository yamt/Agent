package org.eclipse.iofog.connector_client;

public class ConnectorProducerConfig {
    private Integer connectorId;
    private String passKey;

    public ConnectorProducerConfig(Integer connectorId, String passKey) {
        this.connectorId = connectorId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConnectorProducerConfig that = (ConnectorProducerConfig) o;

        if (!connectorId.equals(that.connectorId)) return false;
        return passKey.equals(that.passKey);
    }

    @Override
    public int hashCode() {
        int result = connectorId.hashCode();
        result = 31 * result + passKey.hashCode();
        return result;
    }
}
