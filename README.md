# About

Metaslurper is a command-line tool that harvests digital object properties and
metadata from one or more source services, normalizes it, and uploads it to a
sink service. It supports:

* Efficient streaming of large numbers of entities from any number of source
  services
* Throttling
* Multi-threaded harvesting
* Incremental harvesting
* Harvest count limits

Support for new source and sink services is straightforward to implement.

Metaslurper generally passes along whatever key-value entity metadata the
source services make available to the sink service without modifying it. The
sink service decides what to do with these disparate elements: which ones to
keep, how to map them, etc. This enables the harvester to be written in a
generalized way and run with little configuration (other than needing to know
the URLs and authentication info for the various endpoints) and in conjunction
with pretty much any metadata mapping process.

```
                             command-line invocation
                        (for development & demonstration)
                                        |
                                        V
------------------               ---------------                ----------------
|                |               |             |   invocation   |              |
|                |    queries    |             | <------------- |              |
|                | <------------ |             |                |              |
| source service |               | metaslurper |    content     | sink service |
|                |               |             | -------------> |              |
|                |    content    |             |                |              |
|                | ------------> |             | status updates |              |
|                |               |             | -------------> |              |
------------------               ---------------                ----------------
```

Sink services are modular, too. Currently, two are available:

1. [Metaslurp](https://github.com/medusa-project/metaslurp) (by
interacting with its [HTTP API](https://metadata.library.illinois.edu/api/v1))
2. TestSinkService, which ingests content into nowhere

# Requirements

The only requirements are a JDK (see `docker/Dockerfile` for required version)
and Maven. CPU and memory requirements are minimal.

Docker is required for deployment to AWS ECR. See "AWS ECS Notes" below.

# Build

`$ mvn clean package -DskipTests`

# Configure

Service configuration is sourced from the environment. The following variables
are available:

* Source services
    * Illinois Data Bank
        * `SERVICE_SOURCE_IDB_KEY`
        * `SERVICE_SOURCE_IDB_ENDPOINT`
    * Illinois Digital Library
        * `SERVICE_SOURCE_DLS_KEY`
        * `SERVICE_SOURCE_DLS_ENDPOINT`
        * `SERVICE_SOURCE_DLS_USERNAME`
        * `SERVICE_SOURCE_DLS_SECRET`
    * Illinois Digital Newspaper Collections
        * `SERVICE_SOURCE_IDNC_KEY`
        * `SERVICE_SOURCE_IDNC_ENDPOINT`
        * `SERVICE_SOURCE_IDNC_HARVEST_SCRIPT_URI`
    * IDEALS
        * `SERVICE_SOURCE_IDEALS_KEY`
        * `SERVICE_SOURCE_IDEALS_ENDPOINT`
    * Medusa Book Tracker
        * `SERVICE_SOURCE_BOOK_TRACKER_KEY`
        * `SERVICE_SOURCE_BOOK_TRACKER_ENDPOINT`
* Sink services
    * Metaslurp
        * `SERVICE_SINK_METASLURP_KEY`
        * `SERVICE_SINK_METASLURP_ENDPOINT`
        * `SERVICE_SINK_METASLURP_USERNAME`
        * `SERVICE_SINK_METASLURP_SECRET`
        * `SERVICE_SINK_METASLURP_HARVEST_KEY` (if not set, a new harvest will
          be initiated)
        * `SERVICE_SINK_METASLURP_INDEX` (if not set, the default
          index is used)

# Run

## On the command line

Invoke with no arguments to print a list of available arguments:

```
java -jar target/metaslurper-VERSION.jar
```

Example kitchen-sink invocation:

```
java -jar target/metaslurper-VERSION.jar \
    -source test_source \
    -sink test_sink \
    -log_level info \
    -max_entities 50 \
    -threads 2 \
    -throttle 100 \
    -incremental 1535380169
```

Change `test_source` to a random string to print a list of available service
keys.

## In Docker

`docker-run.sh <environment> <source service key> <sink service key>`

# Test

`mvn test` runs the tests, but you will need to set all of the environment
variables listed above first. You could create a `test.sh` that does that, or
you could put the variables in a `test.env` file and run the tests using
`docker compose --build`.

# Adding services

## Adding a source service

1. Add a class that implements `e.i.l.m.service.SourceService`
2. Add it to the return value of
   `e.i.l.m.service.ServiceFactory.allSourceServices()`

The service will probably require a couple of new configuration keys (a.k.a.
environment variables). In AWS, there are two ways to make these available:

1. Add them to the ECS task definition. If using Metaslurp as a sink, the value
   of its `METASLURPER_ECS_TASK_DEFINITION` environment variable must then be
   changed to this new version, if it is not already using `latest`.
2. Pass them into the task invocation via the ECS API.

## Adding a sink service

1. Add a class that implements `e.i.l.m.service.SinkService`
2. Add it to the return value of
   `e.i.l.m.service.ServiceFactory.allSinkServices()`

# Service implementation notes

* A logger is available via `LoggerFactory.getLogger(Class)`.
* Configuration should be obtained from
  `e.i.l.m.config.Configuration.getInstance()` rather than `System.getenv()`.
* Services are free to use any HTTP client. Most services use
  [OkHttp](https://square.github.io/okhttp/), which is bundled in.

# AWS ECS notes

The general procedure for deploying to ECS is:

1. Install Docker
2. Create an ECR repository, an ECS Fargate cluster, and an ECS task definition
    1. The task definition must define all of the environment variables in
       the "Configuration" section (above)
    2. The task definition also must specify a read-write filesystem (for
       `/tmp` usage)
    3. (At UIUC, all of this is terraformed in
       [demo](https://code.library.illinois.edu/projects/TER/repos/aws-metadata-demo-service/browse)
       and
       [production](https://code.library.illinois.edu/projects/TER/repos/aws-metadata-prod-service/browse).)
3. Install the `aws` command-line tool
4. `cp ecr-push.sh.sample ecr-push.sh` and edit as necessary
5. `ecr-push.sh`

At this point the container is available and tasks are ready to run. One way to
run them is with the `aws` command-line tool, for which a convenient wrapper
script has been written:

`ecs-run-task.sh <environment> <source service key> <sink service key>`

But they can also be invoked via the ECS API or web UI.
