package org.eclipse.iofog.connector_client;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientSession;

import java.util.Map;

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
                        connectorManager.getConnectorConsumers().values().stream()
                            .filter(connectorConsumer ->
                                connectorConsumer.getConfig().getConnectorId() == clientEntry.getKey())
                            .forEach(connectorConsumer -> {
                                try {
                                    if (connectorConsumer.isClosed()) {
                                        client.ejectSession(connectorConsumer.getName());
                                        ClientSession session = client.startSession(connectorConsumer.getName());
                                        connectorConsumer.init(session, connectorConsumer.getConfig());
                                    }
                                } catch (ActiveMQException e) {
                                    logWarning(MODULE_NAME, String.format(
                                        "Connector id %d, session creation error: %s", clientEntry.getKey(), e.getMessage())
                                    );
                                }
                            });
                        connectorManager.getConnectorProducers().values().stream()
                            .filter(connectorProducer ->
                                connectorProducer.getConfig().getConnectorId() == clientEntry.getKey())
                            .forEach(connectorProducer -> {
                                try {
                                    if (connectorProducer.isClosed()) {
                                        client.ejectSession(connectorProducer.getName());
                                        ClientSession session = client.startSession(connectorProducer.getName());
                                        connectorProducer.init(session);
                                    }
                                } catch (ActiveMQException e) {
                                    logWarning(MODULE_NAME, String.format(
                                        "Connector id %d, session creation error: %s", clientEntry.getKey(), e.getMessage())
                                    );
                                }
                            });
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
}
