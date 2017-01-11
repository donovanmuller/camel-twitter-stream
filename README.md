# Camel Twitter Stream

This is a sample [Spring Cloud Stream](https://cloud.spring.io/spring-cloud-stream/)
consisting of three three applications, a `Source`, a `Processor` and a `Sink`, in keeping
with the Spring Cloud Stream default [interfaces](http://docs.spring.io/spring-cloud-stream/docs/current/reference/htmlsingle/#__literal_source_literal_literal_sink_literal_and_literal_processor_literal).

These applications use the [Camel Spring Cloud Stream](https://github.com/donovanmuller/camel-spring-cloud-stream)
component to take advantage
of the Spring Cloud Stream programming model and binder abstraction.

## Stream Definition

The Camel Twitter Stream is defined as follows:

* [Camel Twitter Source](twitter-source) - a Camel consumer of retweets for a specific tweet and emits the screen names of
the retweeters over a Spring Cloud Stream channel. The retweeters are cached in Redis to avoid duplicate
processing on restart of the application.
* [Camel Tweet Processor](tweet-processor) - using the `screenName` header value expected on the received message, over the input channel,
a new tweet in reply to the retweet is rendered using the [Camel Mustache component](http://camel.apache.org/mustache.html)
and sent onward over the output channel.
* [Camel Twitter Sink](twitter-sink) - Creates a new tweet on the configured users timeline
using the received message body, over the input channel, as the tweet text.

## Usage

The stream can be deployed in two ways. 

* run manually as [standalone](#standlalone) Spring Boot applications
* or, orchestrated with [Spring Cloud Data Flow](#spring-cloud-data-flow).

### Binder

The sample applications use the [RabbitMQ binder](https://github.com/spring-cloud/spring-cloud-stream-binder-rabbit)
implementation. If you would prefer a different binder, for example the [Kafka binder](https://github.com/spring-cloud/spring-cloud-stream-binder-kafka)
binder, please update all the corresponding `pom.xml`'s to include the desired binder
starter instead.

It goes without saying that a running instance of the chosen binder
should be running and configured so that the apps can send messages 
over the relevant middleware.

### Redis

The Camel Twitter Sink application prevents retweets being replied to more than once by using
a [Camel Idempotent Consumer](http://camel.apache.org/idempotent-consumer.html).
The idempotent repository sed in the Sink is backed by Redis. Therefore, you must make sure that
you have a running Redis instance available. Use the usual Spring Redis configurations propeties
to configure the connection details.

### Standalone

#### Camel Twitter Sink

> Please take note of Twitter API [Rate Limiting](https://dev.twitter.com/rest/public/rate-limits)

First start the Camel Twitter Sink by downloading the Spring Boot Jar
and running it:

```console
$ wget https://github.com/donovanmuller/camel-twitter-stream/releases/download/1.0-SNAPSHOT/twitter-sink-1.0-SNAPSHOT.jar
$ java -jar twitter-sink-1.0-SNAPSHOT.jar \
    --spring.cloud.stream.bindings.output.destination=twitter-source \
    --twitter.statusId=<statusId> \
    --twitter.pollPeriod=30s \ # 30 seconds by default, note API Rate Limiting
    --twitter.skipDuplicate=true \ # So not reprocess retweets, set to 'false' for testing    
    --server.port=0 \
    --twitter.consumerKey=... \
    --twitter.consumerSecret=... \
    --twitter.accessToken=... \
    --twitter.accessTokenSecret=...
```

note that we provide an alternative destination name for the `output` channel.
This destination becomes the name of the exchange (using RabbitMQ binder).

The Twitter API properties ([see TwitterProperties](twitter-source/src/main/java/io/switchbit/configuration/TwitterProperties.java))
should correspond to the values generated for your Twitter account at [https://dev.twitter.com/apps](https://dev.twitter.com/apps).
This is used to get the retweets for the specific tweet (`statusId`).

#### Camel Tweet Processor

Next, start the Camel Tweet Processor:

```console
$ wget https://github.com/donovanmuller/camel-twitter-stream/releases/download/1.0-SNAPSHOT/tweet-processor-1.0-SNAPSHOT.jar
$ java -jar tweet-processor-1.0-SNAPSHOT.jar --spring.cloud.stream.bindings.input.destination=twitter-source \
    --spring.cloud.stream.bindings.output.destination=twitter-processor \
    --server.port=0
```

Again we change the `input` destination but note that we use the same name
we used on the Camel Twitter Source application, `twitter-source`. 
This is so that Spring Cloud Stream will create a Queue that is bound to the
exchange created by the Source application. I.e. That messages sent on the
`output` channel of the Source application are routed to the Queue created by
Spring Cloud Stream bound to the `input` channel on the Processor application.

We do a similar thing for the `output` channel, naming the destination `twitter-processor`.

##### Tweet Template

You can view/change the content of the Mustache template by editing [tweet.mustache](tweet-processor/src/main/resources/tweet.mustache)

#### Camel Twitter Source

> **NOTE:** Starting the Camel Twitter Source application will start consuming any retweets
for the specified tweet (`statusId`) and will result in corresponding tweets 
(with the tweet content as rendered by the Camel Tweet Processor)
on your timeline. Take caution. :grin:

Finally, start the Camel Twitter Source:

```console
$ wget https://github.com/donovanmuller/camel-twitter-stream/releases/download/1.0-SNAPSHOT/twitter-sink-1.0-SNAPSHOT.jar
$ java -jar twitter-sink-1.0-SNAPSHOT.jar \
    --spring.cloud.stream.bindings.input.destination=twitter-processor \
    --server.port=0 \
    --twitter.consumerKey=... \
    --twitter.consumerSecret=... \
    --twitter.accessToken=... \
    --twitter.accessTokenSecret=...
```

The Twitter API properties ([see TwitterProperties](twitter-source/src/main/java/io/switchbit/configuration/TwitterProperties.java))
should correspond to the values generated for your account at [https://dev.twitter.com/apps](https://dev.twitter.com/apps).
This is used to create the reply tweet on your timeline.

If there are any existing retweets available for the specified tweet (`statusId`) then those
will be processed _immediately_. New retweets will be processed as per the configured polling interval, every `twitter.pollPeriod`.

### Spring Cloud Data Flow

The entire stream can be defined and deployed much more easily via [Spring Cloud Data Flow](http://cloud.spring.io/spring-cloud-dataflow/).
This example will use the [local deployer](http://docs.spring.io/spring-cloud-dataflow/docs/1.1.1.RELEASE/reference/htmlsingle/)
but please see the other available [deployer implementations](spring-cloud-data-flow-implementations)
if you would like to deploy to Kubernetes, Cloud Foundry, Mesos, etc.

Follow the [Getting Started](http://docs.spring.io/spring-cloud-dataflow/docs/1.1.1.RELEASE/reference/htmlsingle/#getting-started)
section in the reference documentation to get a Spring Cloud Data Flow server running.

> This example uses the **Docker resource** type of the applications. Therefore, you must have a running Docker instance on your machine.
You can find the Docker images on Docker Hub. You could also use the Maven resource type
if you built and installed the project locally using Maven. See [below](#building).

Once you have a server running, open the Data Flow Shell and register the three Camel Twitter Stream apps:

```console
dataflow:>app register --name twitter-source --type source --uri docker:donovanmuller/twitter-source:latest
Successfully registered application 'source:twitter-source'
dataflow:>app register --name tweet-processor --type processor --uri docker:donovanmuller/tweet-processor:latest
Successfully registered application 'processor:tweet-processor'
dataflow:>app register --name twitter-sink --type sink --uri docker:donovanmuller/twitter-sink:latest
Successfully registered application 'sink:twitter-sink'
dataflow:>app list
╔══════════════╤═══════════════╤════════════╤════╗
║    source    │   processor   │    sink    │task║
╠══════════════╪═══════════════╪════════════╪════╣
║twitter-source│tweet-processor│twitter-sink│    ║
╚══════════════╧═══════════════╧════════════╧════╝

dataflow:>stream create --name camel-twitter-stream --definition "twitter-source | tweet-processor | twitter-sink"
Created new stream 'camel-twitter-stream'
dataflow:>stream deploy --name camel-twitter-stream --propertiesFile camel-twitter-stream.properties
Deployment request has been sent for stream 'camel-twitter-stream'
```

where `camel-twitter-stream.properties` is a properties file populated with valid (replace all `...` with the corresponding value) configuration values.
A template for this properties file is available in the [`src/etc/scdf/camel-twitter-stream.properties`](src/etc/scdf/camel-twitter-stream.properties)
directory.

Once all the apps have deployed:

```console
dataflow:>stream list
╔════════════════════╤═══════════════════════════════════════════════╤════════╗
║    Stream Name     │               Stream Definition               │ Status ║
╠════════════════════╪═══════════════════════════════════════════════╪════════╣
║camel-twitter-stream│twitter-source | tweet-processor | twitter-sink│deployed║
╚════════════════════╧═══════════════════════════════════════════════╧════════╝

dataflow:>
```

you should now see retweets being replied too.

## Building

To build the apps and install into your local Maven repository use

```console
$ ./mvnw clean install
```

to build the corresponding Docker images, use:

```console
$ ./mvnw package docker:build -Ddocker.prefix=test
```

where `docker.prefix` is the value used when building the Docker images.
I.e. using the example above, the Docker image name would become `test/twitter-source`.

