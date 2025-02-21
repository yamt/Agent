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

import org.eclipse.iofog.exception.AgentUserException;
import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static java.nio.charset.StandardCharsets.UTF_8;

public class InfoApiHandler implements Callable<FullHttpResponse> {
    private static final String MODULE_NAME = "Local API : Info Api handler";

    private final HttpRequest req;
    private final ByteBuf outputBuffer;
    private final byte[] content;

    public InfoApiHandler(HttpRequest request, ByteBuf outputBuffer, byte[] content) {
        this.req = request;
        this.outputBuffer = outputBuffer;
        this.content = content;
    }

    @Override
    public FullHttpResponse call() throws Exception {
    	LoggingService.logInfo(MODULE_NAME, "Start processing info request");
        if (!ApiHandlerHelpers.validateMethod(this.req, GET)) {
            LoggingService.logError(MODULE_NAME, "Request method not allowed", new AgentUserException("Request method not allowed"));
            return ApiHandlerHelpers.methodNotAllowedResponse();
        }

        if (!ApiHandlerHelpers.validateAccessToken(this.req)) {
            String errorMsg = "Incorrect access token";
            outputBuffer.writeBytes(errorMsg.getBytes(UTF_8));
            LoggingService.logError(MODULE_NAME, "Request method not allowed", new AgentUserException(errorMsg));
            return ApiHandlerHelpers.unauthorizedResponse(outputBuffer, errorMsg);
        }

        try {
            String[] info = Configuration.getConfigReport().split("\\\\n");

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> resultMap = new HashMap<>();
            for (String it : info) {
                String[] infoItem = it.split(" : ");
                String key = infoItem[0].trim().toLowerCase().replace(" ", "-");
                if (key.equals("gps-coordinates(lat,lon)")) {
                    key = "gps-coordinates";
                } else if (key.equals("developer's-mode")) {
                    key = "developer-mode";
                }
                resultMap.put(key, infoItem[1].trim());
            }

            String jsonResult = objectMapper.writeValueAsString(resultMap);
            FullHttpResponse res;
            res = ApiHandlerHelpers.successResponse(outputBuffer, jsonResult);
            res.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
            LoggingService.logInfo(MODULE_NAME, "Finished processing info request");
            return res;
        } catch (Exception e) {
            String errorMsg = "Log message parsing error, " + e.getMessage();
            LoggingService.logError(MODULE_NAME, errorMsg, e);
            return ApiHandlerHelpers.badRequestResponse(outputBuffer, errorMsg);
        }
    }
}
