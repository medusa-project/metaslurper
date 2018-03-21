# About

Metaslurper is a command-line tool that harvests (slurps) digital object
properties and metadata from one or more source services, normalizes it, and
uploads it to a sink service. It supports efficient multi-threaded streaming of
large numbers of entities from any number of source services, and support for
new services is straightforward to implement.

Metaslurper passes along whatever key-value entity metadata the source services
make available to the sink service without modifying it. The sink service
decides what to do with these disparate elements: which ones to keep, how to
map them, etc. This decouples the difficult task of metadata mapping from the
harvester, and enables it to run with minimal user interaction.

Metaslurper is designed to work in conjunction with the
[Metaslurp](https://github.com/medusa-project/metaslurp) sink service, but sink
services are modular, too.

# Requirements

The only requirement is JDK 8+.

# Build

`mvn clean package`

# Run

1. Copy `metaslurper.conf.sample` to `metaslurper.conf` and edit as necessary.
2. Invoke:
```
java -Dedu.illinois.library.metaslurper.config=metaslurper.conf \
    -jar metaslurper-VERSION.jar \
    -Xmx128m -source all -sink metaslurp -threads 2
```

Change `-source all` to a service name to limit the slurping to a specific
service. Use a bogus name to get a list of available service names.

# Adding a source service

1. Add a class that implements `e.i.l.m.service.SourceService`
2. Add it to the return value of
   `e.i.l.m.service.ServiceFactory.allSourceServices()`

# Adding a sink service

1. Add a class that implements `e.i.l.m.service.SinkService`
2. Add it to the return value of
   `e.i.l.m.service.ServiceFactory.allSinkServices()`

# Controlling what information gets harvested

1. Modify the `e.i.l.m.entity.Entity` interface.
2. Modify all of its implementations.
3. Modify all `e.i.l.m.service.SinkService` implementations to understand the
   changes.
