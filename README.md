# Building

`mvn clean package`

# Running

1. Copy `metaslurp.conf.sample` to `metaslurper.conf` and edit as necessary.
2. Invoke:
```
java -Dedu.illinois.library.metaslurper.config=metaslurper.conf \
    -jar metaslurper-VERSION.jar
```

Tack on a `-service service-name` argument to limit the slurping to a specific
service.


# Adding a service

1. Add a class that implements `e.i.l.c.service.Service`
2. Add it to the return value of `e.i.l.c.service.ServiceFactory.allServices()`
