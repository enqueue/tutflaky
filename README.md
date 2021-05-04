# Fault tolerance workshop

This is workshop material for a short hands-on tutorial on fault tolerance in Microservice architecture.

We explore some behavior and strategies using two Microservices. [flaky-server](flaky-server) is a Microservice that is not behaving well. We try to make [flaky-client](flaky-client) which is consuming `flaky-server` services a little bit more robust.

## Motivation

* There are more interdependent Microservices.
* Calls to other Microservices are not as reliable as invoking a method on another EJB, for example.
* A fault in one Microservice can lead to desastrous consequences in the zoo of Microservices, rendering the overall service unusable

## Timeout

_See the timeout_

1. Start `flaky-server` by invoking `./gradlew quarkusDev` in the `flaky-server` directory.
2. Start `flaky-client` by invoking `./gradlew quarkusDev` in the `flaky-client` directory.
3. Hook up JVisualVM or your alternative favourite tool to the `flaky-client` Quarkus process.
4. Run the following command:

```
while [ true ]; do sleep 1; exec `curl -s http://localhost:8080/httpclient/waiter` &  done
```
5. Observe how the number of threads increases. Our `flaky-client` Microservice gets bogged down by waiting threads.

A measure to ensure that our client Microservice will still be handle to serve requests is to fail when a resource does not deliver a result in a certain timeframe.

### HttpClient

In the built-in [HttpClient](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.html), we can specify a timeout [via the builder](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.Builder.html#connectTimeout(java.time.Duration)) or per [request](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpRequest.Builder.html#timeout(java.time.Duration)). Either way, when the timeout is over, the client will throw a [HttpTimeoutException](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpTimeoutException.html], which is a sub-class of `IOException`.

```
http://localhost:8080/httpclient/flaky
http://localhost:8080/httpclient/waiter
http://localhost:8080/httpclient/waitertimeout
```

### JAX-RS Client

When using the great JAX-RS client API (see chapter 5 in [JAX-RS spec](https://download.oracle.com/otndocs/jcp/jaxrs-2_1-final-spec/index.html), and [API documentation](https://eclipse-ee4j.github.io/jaxrs-api/apidocs/2.1.6/javax/ws/rs/client/package-summary.html)), we can set two timeouts when constructing a client instance via the ClientBuilder:

1. [Connect timeout](https://eclipse-ee4j.github.io/jaxrs-api/apidocs/2.1.6/javax/ws/rs/client/ClientBuilder.html#connectTimeout(long,java.util.concurrent.TimeUnit)): This timout relates to establishing the initial connection. Useful when the servers are not available at all. You will see a [TimeoutException](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/TimeoutException.html) wrapped in a [ProcessingException](https://eclipse-ee4j.github.io/jaxrs-api/apidocs/2.1.6/javax/ws/rs/ProcessingException.html).
2. [Read timeout](https://eclipse-ee4j.github.io/jaxrs-api/apidocs/2.1.6/javax/ws/rs/client/ClientBuilder.html#readTimeout(long,java.util.concurrent.TimeUnit)): This timeout comes into play when the initial connection has been established. The thread will wait for response packets for the specified duration before raising a [TimeoutException](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/TimeoutException.html) wrapped in a [ProcessingException](https://eclipse-ee4j.github.io/jaxrs-api/apidocs/2.1.6/javax/ws/rs/ProcessingException.html).

Instead of using the methods on the builder explicitly, you can also create a re-usable [Configuration](https://eclipse-ee4j.github.io/jaxrs-api/apidocs/2.1.6/javax/ws/rs/core/Configuration.html) and pass it along.

```
http://localhost:8080/jaxrs/waiter
http://localhost:8080/jaxrs/waitertimeout
http://localhost:8080/jaxrs/connecttimeout
```

### Microprofile Rest Client

We have enjoyed using the [MicroProfile Rest Client API](https://github.com/eclipse/microprofile-rest-client/releases/tag/1.4.1) to make calls to remote services. We can write an interface and leave the actual HTTP stuff to the implementation and the application server.

MicroProfile Rest Client timeouts are specified via configuration. Again, connect and read timeouts can be specified individually. Try it out by setting `flaky/mp-rest/readTimeout` to your favourite milliseconds value and invoke the `mp/wait` endpoint again. You should see another [SocketTimeoutException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/net/SocketTimeoutException.html) wrapped in a [ProcessingException](https://eclipse-ee4j.github.io/jaxrs-api/apidocs/2.1.6/javax/ws/rs/ProcessingException.html).

```
http://localhost:8080/mp/waiter
```

### Microprofile Fault Tolerance

When the Microservices architecture gained momentum about a decade ago, dealing with fault tolerance became a major topic. Application developers used to use popular libraries like Netflix' [Hystrix](https://github.com/Netflix/Hystrix) or Jonathan Halterman's [failsafe](https://jodah.net/failsafe). The Microprofile community tries to address common Microservice issues, so they issued a vendor neutral specification, [MicroProfile Fault Tolerance](https://github.com/eclipse/microprofile-fault-tolerance/releases/tag/2.1.1).

You can decorate a method with a Microprofile Fault Tolerance annotation, and the implemtnation will take care about the rest (e.g. create a proxy object). The annotations can be used on (almost) any method, not only remote HTTP calls. A CDI context is required.

In our example. we can use the [Timeout](https://download.eclipse.org/microprofile/microprofile-fault-tolerance-2.1.1/apidocs/org/eclipse/microprofile/faulttolerance/Timeout.html) annotation on one of our client methods. The implementation will throw a [TimeoutException](https://download.eclipse.org/microprofile/microprofile-fault-tolerance-2.1.1/apidocs/org/eclipse/microprofile/faulttolerance/exceptions/TimeoutException.html) when the time has run out (default unit is milliseconds). Timeout values can also be set via [configuration properties](https://download.eclipse.org/microprofile/microprofile-fault-tolerance-2.1.1/microprofile-fault-tolerance-spec.html#_config_fault_tolerance_parameters), on different levels. E.g. to set the timeout for all client calls to 1337 milliseconds, you could add `Timeout/value = 1337` to your `application.properties` file -- _I tried this, but it did not work as expected._

```
http://localhost:8080/httpclient/waitermpfault
```

### Summary


> Great, I now know that specifying timeouts is important to make my Microservices
> zoo better. I also know how to do this for a couple of popular client libraries.
> But throwing `TimeoutExceptions` is certainly not _always_ the right reaction of a
> well-behaved Microservice, is it?

You are right. Let's see which other problems we can address. We are using the constructs offered by Microprofile Fault Tolerance, but the concepts are used universally.

> All this timeout stuff becomes superfluous once we adopt the _REACTIVE WAY™_,
> because we won't block any threads.

Sure, the issue of a Microservice being brought to a halt by hanging connections to a downstream Microservice, might become less of an issue. It is still good practice not to leave too much garbage around. More importantly, we still have to learn one of the asynchronous ways, e.g. JAX-RS aynchronous API or Quarkus / Vert.x / Mutiny stuff or...


## Retry

## Circuit Breaker

## Fallback

## Bulkhead
