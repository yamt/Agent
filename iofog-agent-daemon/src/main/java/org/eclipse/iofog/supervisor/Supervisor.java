/*******************************************************************************
 * Copyright (c) 2018 Edgeworx, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Saeid Baghbidi
 * Kilton Hopkins
 *  Ashita Nagar
 *******************************************************************************/
package org.eclipse.iofog.supervisor;

import org.eclipse.iofog.IOFogModule;
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.local_api.LocalApi;
import org.eclipse.iofog.message_bus.MessageBus;
import org.eclipse.iofog.process_manager.ProcessManager;
import org.eclipse.iofog.resource_consumption_manager.ResourceConsumptionManager;
import org.eclipse.iofog.resource_manager.ResourceManager;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.tracking.Tracker;
import org.eclipse.iofog.tracking.TrackingEventType;
import org.eclipse.iofog.tracking.TrackingInfoUtils;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.State.TERMINATED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.eclipse.iofog.utils.Constants.*;
import static org.eclipse.iofog.utils.Constants.ModulesStatus.RUNNING;
import static org.eclipse.iofog.utils.Constants.ModulesStatus.STARTING;

/**
 * Supervisor module
 *
 * @author saeid
 *
 */
public class Supervisor implements IOFogModule {

	private static final String MODULE_NAME = "Supervisor";
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private MessageBus messageBus;
	private Thread localApiThread;
	private LocalApi localApi;

	/**
	 * monitors {@link LocalApi} module status
	 *
	 */
	private Runnable checkLocalApiStatus = () -> {
		Thread.currentThread().setName(Constants.SUPERVISOR_CHECK_LOCAL_API_STATUS);
		logInfo("Check local API status");
		try {
			if (localApiThread != null && localApiThread.getState() == TERMINATED) {
				localApiThread = new Thread(localApi, Constants.LOCAL_API_EVENT);
				logInfo("Start local API : status not running");
				localApiThread.start();
				logInfo("Finished starting local API  ");
			}
		} catch (Exception e) {
			LoggingService.logError(MODULE_NAME, "", new AgentSystemException(e.getMessage(), e));
		}
		logInfo("Finished Checking local API status");
	};

	public Supervisor() {}

	/**
	 * starts Supervisor module
	 *
	 * @throws Exception
	 */
	public void start() throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread(shutdownHook, Constants.SHUTDOWN_HOOK));

        logInfo("Starting Supervisor");
        StatusReporter.start();
        StatusReporter.setSupervisorStatus().setModuleStatus(STATUS_REPORTER, RUNNING);

        StatusReporter.setSupervisorStatus()
                .setDaemonStatus(STARTING)
                .setDaemonLastStart(currentTimeMillis())
                .setOperationDuration(0);

		startModule(ResourceConsumptionManager.getInstance());
		startModule(FieldAgent.getInstance());
		startModule(ProcessManager.getInstance());
		startModule(new ResourceManager());
		startModule(Tracker.getInstance());

        messageBus = MessageBus.getInstance();
        startModule(messageBus);

        localApi = LocalApi.getInstance();
        localApiThread = new Thread(localApi, Constants.LOCAL_API_EVENT);
        localApiThread.start();
        scheduler.scheduleAtFixedRate(checkLocalApiStatus, 0, 10, SECONDS);

        StatusReporter.setSupervisorStatus().setDaemonStatus(RUNNING);
		logInfo("Started Supervisor");
        Tracker.getInstance().handleEvent(TrackingEventType.START, TrackingInfoUtils.getStartTrackingInfo());

        operationDuration();
    }

	private void startModule(IOFogModule ioFogModule) throws Exception {
        logInfo(" Starting " + ioFogModule.getModuleName());
        StatusReporter.setSupervisorStatus().setModuleStatus(ioFogModule.getModuleIndex(), STARTING);
        ioFogModule.start();
        StatusReporter.setSupervisorStatus().setModuleStatus(ioFogModule.getModuleIndex(), RUNNING);
        logInfo(" Started " + ioFogModule.getModuleName());
    }

    private void operationDuration(){
    	logInfo(" Start checking operation duration ");
        while (true) {
			StatusReporter.setSupervisorStatus()
				.setOperationDuration(currentTimeMillis());
            try {
                Thread.sleep(Configuration.getStatusReportFreqSeconds() * 1000);
            } catch (InterruptedException e) {
                logError("Error checking operation duration", new AgentSystemException("Error checking operation duration", e));
                System.exit(1);
            }
            logInfo(" Finished checking operation duration ");
        }
    }

	/**
	 * shutdown hook to stop {@link MessageBus} and {@link LocalApi}
	 *
	 */
	private final Runnable shutdownHook = () -> {
		try {
			scheduler.shutdownNow();
			if (localApi != null)
				localApi.stopServer();
			if (messageBus != null)
				messageBus.stop();
		} catch (Exception e) {
			LoggingService.logError(MODULE_NAME, "Error in shutdown hook to stop message bus and local api",
					new AgentSystemException("Error in shutdown hook to stop message bus and local api", e));
		}
	};

	@Override
	public int getModuleIndex() {
		return MESSAGE_BUS;
	}

	@Override
	public String getModuleName() {
		return MODULE_NAME;
	}

}
