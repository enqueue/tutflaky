/*
 * Copyright (c) 2021 Riege Software. All rights reserved.
 * Use is subject to license terms.
 */
package com.riege.connect.tutorials.flakyclient.resource;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Call the flaky service using JAX-RS Client API
 * @author Felix Mueller
 */
@Path("jaxrs")
public class FlakyClientJAXRSClientResource {

    @ConfigProperty(name = "com.riege.connect.tutorials.flakyclient.server.http.port")
    String flakyServerPort;

    @GET
    @Path("waiter")
    public boolean callWait() {
        Client client = ClientBuilder.newClient();
        client.target("http://localhost:" + flakyServerPort + "/wait")
            .request().get();
        return true;
    }

    @GET
    @Path("waitertimeout")
    public boolean callWaitTimeout() {
        Client client = ClientBuilder.newBuilder()
            .readTimeout(200, TimeUnit.MILLISECONDS)
            .build();
        client.target("http://localhost:" + flakyServerPort + "/wait")
            .request().get();
        return true;
    }

    @GET
    @Path("connecttimeout")
    public boolean callConnectTimeout() {
        Client client = ClientBuilder.newBuilder()
            .connectTimeout(2200, TimeUnit.MILLISECONDS)
            .build();
        client.target("http://xdispatcher.dev.riege.de:4815")
            .request().get();
        return true;
    }

}
