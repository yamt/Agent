/*******************************************************************************
 * Copyright (c) 2019 Edgeworx, Inc.
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
package org.eclipse.iofog.local_api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;

import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.exception.AgentUserException;
import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.utils.logging.LoggingService;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static io.netty.handler.codec.http.HttpMethod.POST;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.eclipse.iofog.utils.CmdProperties.*;
import static org.eclipse.iofog.utils.configuration.Configuration.setConfig;

public class ProvisionApiHandler implements Callable<FullHttpResponse> {
    private static final String MODULE_NAME = "Local API : Provision Api Handler";
    private final String PROVISIONING_KEY = "provisioning-key";

    private final HttpRequest req;
    private final ByteBuf outputBuffer;
    private final byte[] content;

    public ProvisionApiHandler(HttpRequest request, ByteBuf outputBuffer, byte[] content) {
        this.req = request;
        this.outputBuffer = outputBuffer;
        this.content = content;
    }

    @Override
    public FullHttpResponse call() throws Exception {
    	LoggingService.logInfo(MODULE_NAME, "Start processing request in Provision Api Handler");
        if (!ApiHandlerHelpers.validateMethod(this.req, POST)) {
            LoggingService.logError(MODULE_NAME, "Request method not allowed", 
            		new AgentUserException("Request method not allowed", new Exception()));
            return ApiHandlerHelpers.methodNotAllowedResponse();
        }

        final String contentTypeError = ApiHandlerHelpers.validateContentType(this.req, "application/json");
        if (contentTypeError != null) {
            LoggingService.logError(MODULE_NAME, contentTypeError, 
            		new AgentUserException(contentTypeError, new Exception()));
            return ApiHandlerHelpers.badRequestResponse(outputBuffer, contentTypeError);
        }

        if (!ApiHandlerHelpers.validateAccessToken(this.req)) {
            String errorMsg = "Incorrect access token";
            outputBuffer.writeBytes(errorMsg.getBytes(UTF_8));
            LoggingService.logError(MODULE_NAME, contentTypeError, 
            		new AgentUserException("Incorrect access token", new Exception()));
            return ApiHandlerHelpers.unauthorizedResponse(outputBuffer, errorMsg);
        }

        try {
            String msgString = new String(content, UTF_8);
            JsonReader reader = Json.createReader(new StringReader(msgString));
            JsonObject provisionRequest = reader.readObject();

            if (!provisionRequest.containsKey(PROVISIONING_KEY)) {
                return ApiHandlerHelpers.badRequestResponse(outputBuffer, "Missing required property '" + PROVISIONING_KEY + "'");
            }

            String provisioningKey = provisionRequest.getString(PROVISIONING_KEY);
            JsonObject provisioningResult = FieldAgent.getInstance().provision(provisioningKey);

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> resultMap = new HashMap<>();
            for (String messageKey : provisioningResult.keySet()) {
                resultMap.put(messageKey, provisioningResult.getString(messageKey));
            }
            String jsonResult = objectMapper.writeValueAsString(resultMap);
            FullHttpResponse res;
            if (resultMap.get("status").equals("failed")) {
                res = ApiHandlerHelpers.internalServerErrorResponse(outputBuffer, jsonResult);
            } else {
                res = ApiHandlerHelpers.successResponse(outputBuffer, jsonResult);
            }
            res.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
            LoggingService.logInfo(MODULE_NAME, "Finished processing request in Provision Api Handler");
            return res;
        } catch (Exception e) {
            String errorMsg = "Log message parsing error, " + e.getMessage();
            LoggingService.logError(MODULE_NAME, errorMsg, new AgentSystemException(e.getMessage(), e));
            return ApiHandlerHelpers.badRequestResponse(outputBuffer, errorMsg);
        }
    }
}
