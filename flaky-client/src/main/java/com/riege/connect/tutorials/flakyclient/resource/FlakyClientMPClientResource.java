/*
 * Copyright (c) 2021 Riege Software. All rights reserved.
 * Use is subject to license terms.
 */
package com.riege.connect.tutorials.flakyclient.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * Call the flaky service using a MicroProfile Rest Client instance
 * @author Felix Mueller
 */
@Path("mp")
public class FlakyClientMPClientResource {

    @RestClient
    @Inject
    FlakyRestClient client;

    @GET
    @Path("waiter")
    public boolean callWait() {
        client.waiter();
        return true;
    }

}
