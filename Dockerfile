FROM openjdk:8u131-jdk-alpine

WORKDIR .

ADD ./target/metaslurper-1.0-SNAPSHOT.jar .
ADD ./metaslurper.conf .

CMD ["java", "-Xmx128m", \
    "-Dedu.illinois.library.metaslurper.config=metaslurper.conf", \
    "-jar", "metaslurper-1.0-SNAPSHOT.jar", \
    "-source", "all", "-sink", "metaslurp", "-threads", "2"]
