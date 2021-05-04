/*
 * Copyright (c) 2021 Riege Software. All rights reserved.
 * Use is subject to license terms.
 */
package com.riege.connect.tutorials.flakyclient.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * MP Rest Client for calling the dreaded Flaky Server
 * @author Felix Mueller
 */
@RegisterRestClient(configKey = "flaky")
public interface FlakyRestClient {

    @GET
    @Path("wait")
    String waiter();

    @GET
    @Path("mixed")
    String flaky();

}
