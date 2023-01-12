[![Build](https://github.com/driessamyn/kafkasnoop/actions/workflows/gradle.yml/badge.svg)](https://github.com/driessamyn/kafkasnoop/actions/workflows/gradle.yml) ![Licence](https://img.shields.io/github/license/driessamyn/kafkasnoop?style=plastic)

# kafkasnoop

Frictionless kafka message snooping for testing and debugging.

## What is it?

KafkaSnoop is intended to be an easy to set up tool for snooping on (and publishing) Kafka Messages. The focus is on testing, local development, troubleshooting, and specifically _not_ optimised for security or performance.
** Do not use this in a production or sensitive environment! **

## How does it work?

KafkaSnoop exposes a HTTP REST & Web Socket interface into your Kafka Cluster so that you can easily snoop on messages and/or publish messages to it. This may be helpful while developing or testing an application that integerates with Kafka.

There are 2 components to KafkaSnoop:

* The HTTP API service - exposes an easy to use, _insecure_, interface to your Kafka cluster
* The message deserialiser service - makes sure messages are presented in readable format, more on that later

## Is it really frictionless?

### It depends ...

KafkaSnoop is designed to work with as minimal configuration or customisation as possible. For example, if all messages are in JSON format, simply start up the KafkaSnoop HTTP component pointed to your Kafka Broker and _it will work_.

```shell
kafkasnoop.http --broker localhost:9092
```

or see the [docker example](examples/simple-json/README.md).

### What if I don't use JSON

KafkaSnoop is not psychic. When messages are in binary format, you need to help it along a little bit. This is where the _message deserialser service_ comes in. This service is responsible for turning the binary message into something readable. KafkaSnoop will make this as easy as it can, but it needs a little bit help.
The following scenarios are supported:

### I'm using Avro

That's great. It means you need to do a couple of things to help KafkaSnoop. Firstly you need to provivde it with the avro schemas for the messages on Kafka. These should be provided to the _message deserialser service_ as a directory or zip file:

```
kafkasnoop-deserialiser-TBC --schemaDir /path/to/schema/files
```

or:

```
kafkasnoop-deserialiser-TBC --schemaZip /path/to/schema/avro-schemas.zip
```

With the above configuratioin KafkaSnoop will make _best efforts_ to deserialise your messagages using the schemas give. 
But remember, KafkaSnoop is not psychic, and AVRO messages don't generally contain schema information, so you may need to help it along a bit.
By default, KafakSnoop will therefor try all the schemas it knows of for all messages and return messages like so:

```json
{
  "key": {
    "payloads": [
      {
        "payload": ...,
        "schema", "<schema-used>"
       },
       {
        "payload": ...,
        "schema", "<alternative-schema-used>"
       },
    ]
  },
  "value": ...
}
```

This could be quite annoying when you have a lot of schemas, right?
For this reason, you can give KakfaSnoop a hint when querying the messages. For example:

```
http://localhost:8888/api/<your-topic>?schema=<schema-name-to-use>
```

### But I already have a schema registry

Great. All you need to do is expose that schema registry to KafkaSnoop through a custom _message deserialser service_. The good news is that this uses a very simple API for KafkaSnoop to use, and can be implemented in any language.
This service needs to implement a POST endpoint on `/api/deserialise` that takes the following payload:

```json
{
  "topic": "<topic-name>",
  "partition": "<partition>",
  "key": "<message-payload>",
  "value": "<message-payload>",
}
```

`message-payload` is a base64 encoded representation of the binary message payload.

The service needs to return a JSON message of the following structure:

```json
{
  "topic": "<topic-name>",
  "partition": "<partition>",
  "key": { ... json representation of deserialised message ... },
  "value": { ... json representation of deserialised message ... },
}
```

The above payload can be extended with additional properties (e.g. schema used), and will be returned by KafkaSnoop in its entirety. 

### I don't use AVRO

No problem. You can use to above method of providing a custom _message deserialser service_ to turn your binary message into a JSON representation.

## Publish kafkasnoop-wrap docker image

To create and publish docker image of `kafkasnoo-wrap`, the following two commands need to be run:

```
./gradlew copyAvroSchemaDependencies
```
This custom task will copy all avro schemas out of `corda-api`'s `corda-avro-schema` dependency and prepare them to copied inside the docker image.

```
./gradlew :http:snoop-wrap:jib
```
This command creates and pushes the image to the specified docker registry.
The configurations are set in module snoop-wrap `build.gradle.kts` in the `jib` block.<br />
NOTE: environment variables `CORDA_ARTIFACTORY_USERNAME` and `CORDA_ARTIFACTORY_PASSWORD` need to be set.