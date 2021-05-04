# Fault tolerance workshop

This is workshop material for a short hands-on tutorial on fault tolerance in Microservice architecture.

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

In the built-in [HttpClient|https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.html), we can specify a timeout [via the builder](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.Builder.html#connectTimeout(java.time.Duration)) or per [request](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpRequest.Builder.html#timeout(java.time.Duration)). Either way, when the timeout is over, the client will throw a [HttpTimeoutException|https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpTimeoutException.html], which is a sub-class of `IOException`.

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
