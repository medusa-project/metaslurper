# About

Metaslurper is a command-line tool that harvests (slurps) digital object
properties and metadata from one or more source services, normalizes it, and
uploads it to a sink service. It supports efficient multi-threaded streaming of
large numbers of entities from any number of source services, and support for
new services is straightforward to implement.

Metaslurper generally passes along whatever key-value entity metadata the
source services make available to the sink service without modifying it. The
sink service decides what to do with these disparate elements: which ones to
keep, how to map them, etc. This enables the harvester to run with minimal
configuration, and in conjunction with pretty much any mapping process.

Metaslurper is designed to work in conjunction with the
[Metaslurp](https://github.com/medusa-project/metaslurp) sink service, but sink
services are modular, too.

# Requirements

The only requirements are JDK 8+ and Maven 3. CPU and memory requirements are
minimal.

Docker is required for deployment to AWS ECR. See "AWS ECS Notes" below.

# Build

`mvn clean package -DskipTests`

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
      * `SERVICE_SINK_METASLURP_HARVEST_KEY` (optional; if not set, a new
        harvest will be created)

# Run

## On the command line

Invoke with no arguments to print a list of available arguments:

```
java -jar target/metaslurper-VERSION.jar
```

Example invocation:

```
java -jar target/metaslurper-VERSION.jar \
    -source test \
    -sink $SERVICE_SINK_METASLURP_KEY \
    -log_level info \
    -threads 2 \
    -incremental 1535380169
```

`test` is a built-in test source service that will harvest some fake content.
Change it to a random string to print a list of available service keys.

## In Docker

1. `cp docker-run.sh.sample docker-run.sh` and edit as necessary
2. `docker-run.sh <source service key> <sink service key>`

# Adding services

## Adding a source service

1. Add a class that implements `e.i.l.m.service.SourceService`
2. Add it to the return value of
   `e.i.l.m.service.ServiceFactory.allSourceServices()`

Probably the service will require a couple of new configuration keys. In AWS,
these will need to be added as environment variables to the ECS task
definition. If using Metaslurp as a sink, the value of its
`METASLURPER_ECS_TASK_DEFINITION` environment variable must then be changed to
this new version.

## Adding a sink service

1. Add a class that implements `e.i.l.m.service.SinkService`
2. Add it to the return value of
   `e.i.l.m.service.ServiceFactory.allSinkServices()`

# Service implementation notes

* A logger is available via `LoggerFactory.getLogger(Class)`.
* Configuration should be obtained from
  `e.i.l.m.config.Configuration.getInstance()` rather than `System.getenv()`.

# AWS ECS notes

Metaslurper can run in AWS ECS. The general procedure for deploying is:

1. Install Docker
2. Create an ECR repository, an ECS Fargate cluster, and an ECS task definition
    1. The task definition must define all of the environment variables in
       the "Configuration" section (above)
    2. The task definition also must specify a read-write filesystem (for
       `/tmp` usage)
3. Install the `aws` command-line tool
4. `cp ecr-push.sh.sample ecr-push.sh` and edit as necessary
5. `ecr-push.sh`

At this point the container is available and tasks are ready to run. One way to
run them is with the `aws` command-line tool, for which a convenient wrapper
script has been written:

`ecs-run-task.sh <source service key> <sink service key>`

But they can also be invoked via the AWS web UI or API.
