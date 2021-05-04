package com.riege.connect.tutorials.resource;

import java.util.Random;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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