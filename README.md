Metaslurper is a command-line tool for retrieving (slurping) content from one
or more source endpoints, transforming it, and uploading it to a sink endpoint.

# Building

`mvn clean package`

# Running

1. Copy `metaslurp.conf.sample` to `metaslurper.conf` and edit as necessary.
2. Invoke:
```
java -Dedu.illinois.library.metaslurper.config=metaslurper.conf \
    -jar metaslurper-VERSION.jar -service all
```

Change `all` to a service name to limit the slurping to a specific service.
Use `bogus` to get a list of service names.

# Adding a service

1. Add a class that implements `e.i.l.m.service.SourceService`
2. Add it to the return value of `e.i.l.m.service.ServiceFactory.allServices()`
