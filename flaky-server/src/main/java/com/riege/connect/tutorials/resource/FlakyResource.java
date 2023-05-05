package com.riege.connect.tutorials.resource;

import java.util.Random;
import java.util.logging.Logger;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Path("/")
public class FlakyResource {

    private static final Logger LOGGER =
        Logger.getLogger(FlakyResource.class.getName());

    @GET
    @Path("mixed")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        maybeFail();
        try {
            randomDelay();
            return "Hello World";
        } catch (InterruptedException ie) {
            LOGGER.severe("interrupted");
            return null;
        }
    }

    @GET
    @Path("wait")
    public String longRunner() {
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException ie) {
            LOGGER.severe("interrupted");
        }
        return "wokeup";
    }

    @GET
    @Path("bad")
    public Response badRequest() {
        return Response
            .status(Status.BAD_REQUEST)
            .build();
    }

    private void maybeFail() {
        if (new Random().nextBoolean()) {
            LOGGER.severe("I am failing.");
            throw new RuntimeException("Failure");
        }
    }

    private void randomDelay() throws InterruptedException {
        Thread.sleep(new Random().nextInt(500));
    }

}