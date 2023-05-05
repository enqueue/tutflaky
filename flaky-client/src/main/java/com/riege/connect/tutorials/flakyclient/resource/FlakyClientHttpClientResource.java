/*
 * Copyright (c) 2021 Riege Software. All rights reserved.
 * Use is subject to license terms.
 */
package com.riege.connect.tutorials.flakyclient.resource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.logging.Logger;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Timeout;

/**
 * Call the server using Java built-in {@link HttpClient} and be very robust
 * about it
 *
 * @author Felix Mueller
 */
@Path("httpclient")
public class FlakyClientHttpClientResource {

    private HttpClient httpClient;

    @ConfigProperty(name = "com.riege.connect.tutorials.flakyclient.server.http.port")
    String flakyServerPort;

    public FlakyClientHttpClientResource() {
        httpClient = HttpClient.newBuilder().build();
    }

    @GET
    @Path("flaky")
    public boolean callServer() throws Exception {
        HttpResponse<Void> response = httpClient.send(
            HttpRequest.newBuilder(
                    URI.create("http://localhost:" + flakyServerPort + "/mixed"))
                .build(),
                BodyHandlers.discarding());
        Logger.getLogger(FlakyClientHttpClientResource.class.getName())
            .info("Response Status: " + response.statusCode());
        if (response.statusCode() == 200) {
            return true;
        }
        throw new RuntimeException("kaputt");
    }

    @GET
    @Path("waiter")
    public boolean callWait() throws Exception {
        HttpResponse<Void> response = httpClient.send(
            HttpRequest.newBuilder(
                    URI.create("http://localhost:" + flakyServerPort + "/wait"))
                .build(),
                BodyHandlers.discarding());
        Logger.getLogger(FlakyClientHttpClientResource.class.getName())
            .info("Response Status: " + response.statusCode());
        if (response.statusCode() == 200) {
            return true;
        }
        throw new RuntimeException(response.toString());
    }

    @GET
    @Path("waitertimeout")
    public boolean callWaitTimeout() throws Exception {
        HttpResponse<String> response;
        try {
            response = httpClient.send(
                HttpRequest.newBuilder(
                    URI.create("http://localhost:" + flakyServerPort + "/wait"))
                .timeout(Duration.ofMillis(200L))
                .build(),
            BodyHandlers.ofString());
        } catch (HttpTimeoutException htoe) {
            return false;
        }
        Logger.getLogger(FlakyClientHttpClientResource.class.getName())
            .info("Response Status: " + response.statusCode());
        if (response.statusCode() == 200) {
            return true;
        }
        throw new RuntimeException("kaputt");
    }

    @Timeout(200) // MP Fault Tolerance
    @GET
    @Path("waitermpfault")
    public boolean callWaitTimeoutMPFault() throws Exception {
       HttpResponse<Void> response = httpClient.send(
            HttpRequest.newBuilder(
                URI.create("http://localhost:" + flakyServerPort + "/wait"))
            .build(),
            BodyHandlers.discarding());
       Logger.getLogger(FlakyClientHttpClientResource.class.getName())
           .info("Response Status: " + response.statusCode());
       return true;
    }

}
