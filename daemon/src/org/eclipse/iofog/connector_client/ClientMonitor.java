package org.eclipse.iofog.connector_client;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ActiveMQNonExistentQueueException;
import org.apache.activemq.artemis.api.core.client.ClientSession;

import java.util.Map;

import static org.eclipse.iofog.utils.logging.LoggingService.logInfo;
import static org.eclipse.iofog.utils.logging.LoggingService.logWarning;

public class ClientMonitor implements Runnable {

    public static final String MODULE_NAME = "Client Monitor";

    private ConnectorManager connectorManager;

    public ClientMonitor(ConnectorManager connectorManager) {
        this.connectorManager = connectorManager;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(10000);

                for (Map.Entry<Integer, Client> clientEntry : connectorManager.getClients().entrySet()) {
                    Client client = clientEntry.getValue();

                    try {
                        if (client.getCsf() == null || client.getCsf().isClosed()) {
                            client.init(client.getConfig());
                        }
                        monitorConsumers(clientEntry.getKey(), client);
                        monitorProducers(clientEntry.getKey(), client);
                    } catch (Exception e) {
                        logWarning(MODULE_NAME,
                            String.format("Connector id %d, session factory creation error: %s", clientEntry.getKey(), e.getMessage())
                        );
                    }

                }
            } catch (InterruptedException ex) {
                logWarning(MODULE_NAME, ex.getMessage());
            }
        }
    }

    private void monitorConsumers(int connectorId, Client client) {
        connectorManager.getConnectorConsumers().values().stream()
            .filter(connectorConsumer ->
                connectorConsumer.getConfig().getConnectorId() == connectorId)
            .forEach(connectorConsumer -> {
                try {
                    if (connectorConsumer.isClosed()) {
                        client.ejectSession(connectorConsumer.getName());
                        ClientSession session = client.startSession(connectorConsumer.getName());
                        connectorConsumer.init(session, connectorConsumer.getConfig());
                    }
                } catch (ActiveMQNonExistentQueueException e) {
                    logInfo(MODULE_NAME, String.format(
                        "Connector id %d, message: '%s'", connectorId, e.getMessage())
                    );
                } catch (ActiveMQException e) {
                    logWarning(MODULE_NAME, String.format(
                        "Connector id %d, session creation error: %s", connectorId, e.getMessage())
                    );
                }
            });
    }

    private void monitorProducers(int connectorId, Client client) {
        connectorManager.getConnectorProducers().values().stream()
            .filter(connectorProducer ->
                connectorProducer.getConfig().getConnectorId() == connectorId)
            .forEach(connectorProducer -> {
                try {
                    if (connectorProducer.isClosed()) {
                        client.ejectSession(connectorProducer.getName());
                        ClientSession session = client.startSession(connectorProducer.getName());
                        connectorProducer.init(session);
                    }
                } catch (ActiveMQNonExistentQueueException e) {
                    logInfo(MODULE_NAME, String.format(
                        "Connector id %d, message: '%s'", connectorId, e.getMessage())
                    );
                } catch (ActiveMQException e) {
                    logWarning(MODULE_NAME, String.format(
                        "Connector id %d, session creation error: %s", connectorId, e.getMessage())
                    );
                }
            });
    }
}
