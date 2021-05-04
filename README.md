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

In the built-in [HttpClient](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.html), we can specify a timeout [via the builder](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.Builder.html#connectTimeout(java.time.Duration)) or per [request](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpRequest.Builder.html#timeout(java.time.Duration)). Either way, when the timeout is over, the client will throw a [HttpTimeoutException](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpTimeoutException.html), which is a sub-class of `IOException`.

```
http://localhost:8080/httpclient/flaky
http://localhost:8080/httpclient/waiter
http://localhost:8080/httpclient/waitertimeout
```

### JAX-RS Client

When using the great JAX-RS client API (see chapter 5 in [JAX-RS spec](https://download.oracle.com/otndocs/jcp/jaxrs-2_1-final-spec/index.html), and [API documentation](https://eclipse-ee4j.github.io/jaxrs-api/apidocs/2.1.6/javax/ws/rs/client/package-summary.html)), we can set two timeouts when constructing a client instance via the ClientBuilder:

1. [Connect timeout](https://eclipse-ee4j.github.io/jaxrs-api/apidocs/2.1.6/javax/ws/rs/client/ClientBuilder.html#connectTimeout(long,java.util.concurrent.TimeUnit)): This timout relates to establishing the initial connection. Useful when the servers are not available at all. You will see a [TimeoutException](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/TimeoutException.html) wrapped in a [ProcessingException](https://eclipse-ee4j.github.io/jaxrs-api/apidocs/2.1.6/javax/ws/rs/ProcessingException.html).
2. [Read timeout](https://eclipse-ee4j.github.io/jaxrs-api/apidocs/2.1.6/javax/ws/rs/client/ClientBuilder.html#readTimeout(long,java.util.concurrent.TimeUnit)): This timeout comes into play when the initial connection has been established. The thread will wait for response packets for the specified duration before raising a [TimeoutException](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/TimeoutException.html) wrapped in a [ProcessingException](https://eclipse-ee4j.github.io/jaxrs-api/apidocs/2.1.6/javax/ws/rs/ProcessingException.html).

We should always care about both time outs.

Instead of using the methods on the builder explicitly, you can also create a re-usable [Configuration](https://eclipse-ee4j.github.io/jaxrs-api/apidocs/2.1.6/javax/ws/rs/core/Configuration.html) and pass it along.

```
http://localhost:8080/jaxrs/waiter
http://localhost:8080/jaxrs/waitertimeout
http://localhost:8080/jaxrs/connecttimeout
```

### MicroProfile Rest Client

We have enjoyed using the [MicroProfile Rest Client API](https://github.com/eclipse/microprofile-rest-client/releases/tag/1.4.1) to make calls to remote services. We can write an interface and leave the actual HTTP stuff to the implementation and the application server.

MicroProfile Rest Client timeouts are specified via configuration. Again, connect and read timeouts can be specified individually. Try it out by setting `flaky/mp-rest/readTimeout` to your favourite milliseconds value and invoke the `mp/wait` endpoint again. You should see another [SocketTimeoutException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/net/SocketTimeoutException.html) wrapped in a [ProcessingException](https://eclipse-ee4j.github.io/jaxrs-api/apidocs/2.1.6/javax/ws/rs/ProcessingException.html).

```
http://localhost:8080/mp/waiter
```

### MicroProfile Fault Tolerance

When the Microservices architecture gained momentum about a decade ago, dealing with fault tolerance became a major topic. Application developers used to use popular libraries like Netflix' [Hystrix](https://github.com/Netflix/Hystrix) or Jonathan Halterman's [failsafe](https://jodah.net/failsafe). The MicroProfile community tries to address common Microservice issues, so they issued a vendor neutral specification, [MicroProfile Fault Tolerance](https://github.com/eclipse/microprofile-fault-tolerance/releases/tag/2.1.1).

You can decorate a method with a MicroProfile Fault Tolerance annotation, and the implemtnation will take care about the rest (e.g. create a proxy object). The annotations can be used on (almost) any method, not only remote HTTP calls. A CDI context is required.

In our example. we can use the [Timeout](https://download.eclipse.org/microprofile/microprofile-fault-tolerance-2.1.1/apidocs/org/eclipse/microprofile/faulttolerance/Timeout.html) annotation on one of our client methods. The implementation will throw a [TimeoutException](https://download.eclipse.org/microprofile/microprofile-fault-tolerance-2.1.1/apidocs/org/eclipse/microprofile/faulttolerance/exceptions/TimeoutException.html) when the time has run out (default unit is milliseconds). Timeout values can also be set via [configuration properties](https://download.eclipse.org/microprofile/microprofile-fault-tolerance-2.1.1/microprofile-fault-tolerance-spec.html#_config_fault_tolerance_parameters), on different levels. E.g. to set the timeout for all client calls to 1337 milliseconds, you could add `Timeout/value = 1337` to your `application.properties` file -- _I tried this, but it did not work as expected._

```
http://localhost:8080/httpclient/waitermpfault
```

### Summary


> Great, I now know that specifying timeouts is important to make my Microservices
> zoo better. I also know how to do this for a couple of popular client libraries.
> But throwing `TimeoutExceptions` is certainly not _always_ the right reaction of a
> well-behaved Microservice, is it?

You are right. Let's see which other problems we can address. We are using the constructs offered by MicroProfile Fault Tolerance, but the concepts are used universally.

> All this timeout stuff becomes superfluous once we adopt the _REACTIVE WAY™_,
> because we won't block any threads.

Sure, the issue of a Microservice being brought to a halt by hanging connections to a downstream Microservice, might become less of an issue. It is still good practice not to leave too much garbage around. More importantly, we still have to learn one of the asynchronous ways, e.g. JAX-RS aynchronous API or Quarkus / Vert.x / Mutiny stuff or...

## Retry

When experiencing a failure during a remote call, we sometimes hope that a following call will be successful. This is what the [Retry](https://download.eclipse.org/microprofile/microprofile-fault-tolerance-2.1.1/microprofile-fault-tolerance-spec.html#retry) annotation is good for. The spec says:

> In order to recover from a brief network glitch, @Retry
> can be used to invoke the same operation again

The retry policy is activated when an exception in thrown during the call. This is clearly written in the spec:

> When a method returns and the retry policy is present, the following rules are applied:
> * If the method returns normally (doesn’t throw), the result is simply returned.
> * Otherwise, if the thrown object is assignable to any value in the abortOn parameter, the thrown object is rethrown.
> * Otherwise, if the thrown object is assignable to any value in the retryOn parameter, the method call is retried.
> * Otherwise the thrown object is rethrown.

So it depends a little bit on the client we are using to make the request: The regular HttpClient is happy when a response is returned. MicroProfile Rest Client's default `ResponseExceptionMapper` on the other hand will throw a JAX-RS [WebApplicationException](https://docs.oracle.com/javaee/7/api/javax/ws/rs/WebApplicationException.html) when it encounters a response with a status code >= 400 (we _hope_ that it will differentiate between `ClientErrorException` and `ServerErrorException`).

It does _not_ make sense to retry a call to an endpoint when we receive a response in the 400 range, so we should limit the retries to those Exception from which we hope we can recover, e.g. `SocketTimeoutException` or `IOException` in general. This can be achieved using the `retryOn` property of the `@Retry` annotation.

Bonus: Use the metrics integration for understanding what happens. Do not use in production, I would recommend.

```
curl -L http://localhost:8080/metrics | grep ft
```

## Fallback

Instead of using a `try / catch` block or parsing the "bad" responses yourself, you can use the [Fallback](https://download.eclipse.org/microprofile/microprofile-fault-tolerance-2.1.1/microprofile-fault-tolerance-spec.html#fallback) annotation, specifying the method which should be called.

You can limit the fallback to certain Exceptions using the `applyOn` property. This is a natural match with the Exceptions the other MicroProfile Fault Tolerance annotations, e.g. `CircuitBreakerOpenException`.

```
http://localhost:8080/retry/fall
```

## Circuit Breaker

Imagine you have a Microservice that is currently feeling a little bit weak. If this is a service that is called by a cascade of other, dependent Microservices each of which uses a Retry mechanism to try and make up for errors, you end up with a lot more stress on the poor service and the client services will be busy with their retries instead of getting other work done. This is where the [Circuit breaker](https://download.eclipse.org/microprofile/microprofile-fault-tolerance-2.1.1/microprofile-fault-tolerance-spec.html#circuitbreaker) comes into play: Instead of spending more resources on trying to get an answer from and putting more pressure on a downstream service that is probably not able to serve our requests right now, we can back off and perform some default operation for a certain time. After that period we will check again if the downstream service can answer our requests.

You annotate the method with a `@CircuitBreaker` annotation to make use of this pattern. Each Circuit breaker requires resources for bookkeeping.

These are its properties:

Property | Explanation
-------- | -----------
`failureRatio` | Open the circuit, when the ratio of failures is above this threshold (default = 0.5)
`requestVolumeThreshold` | Window size for determining the ratio (amount of invocations) (default = 0.2)
`successThreshold` | When trying to close the circuit again, how many trial calls should we perform?
`delay` | How long should the Circuit breaker remain open for, before trying to close it again?

Remember, that is does probably not make sense to count responses from the 400 range as failures, so you can use the `failOn` property to specify the type of Exception you want to fail on, or the `skipOn` property if you want to ignore certain exceptions.

```
http://localhost:8080/retry/break
```

## Bulkhead

_This is used for static rate limiting. I do not know anything about it._

## Summary

1. Every Microservice call should time out.
2. Think about your response when a downstream service fails: Is there a good default? Do you want to signal an Exception upstream?
3. Think about your response when a downstream service signals a client error (400 range):
* Is your caller responsible? Then perhaps he should get a 400 answer, too.
* Do not retry requests that yielded a 400 response.
* Do not count client errors as Circuit breaker failures.
4. Beware of the default MicroProfile Client Exception Mapper.
5. Every Microservice call should use a Circuit breaker.

## Material

* [Making the Netflix API More Resilient](https://netflixtechblog.com/making-the-netflix-api-more-resilient-a8ec62159c2d)
* [Fault Tolerance in Hight Volume, Distributed Systems](https://netflixtechblog.com/fault-tolerance-in-a-high-volume-distributed-system-91ab4faae74a)
* [Strategies for handling partial failure](https://docs.microsoft.com/en-us/dotnet/architecture/microservices/implement-resilient-applications/partial-failure-strategies)
* [MicroProfile Fault Tolerance tutorials for OpenLiberty](https://developer.ibm.com/videos/build-fault-tolerant-microservices-intro/)
* [Quarkus SmallRye Fault Tolerance Guide](https://quarkus.io/guides/smallrye-fault-tolerance)
