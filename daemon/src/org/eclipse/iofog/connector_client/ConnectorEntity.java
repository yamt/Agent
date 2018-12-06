package org.eclipse.iofog.connector_client;

public class ConnectorEntity {
    protected String name;
    protected ConnectorClient connectorClient;

    public ConnectorEntity(String name, ConnectorClient connectorClient) {
        this.name = name;
        this.connectorClient = connectorClient;
    }
}
