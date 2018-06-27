#
# N.B.: An application JAR file must be built before the image can be created.
# See the README.
#

FROM openjdk:10.0.1-10-jdk

WORKDIR .

COPY ./target/metaslurper-1.0-SNAPSHOT.jar ./metaslurper.jar

# This will need to be overridden at runtime.
CMD ["java", "-jar", "metaslurper.jar", \
    "-source", "test", "-sink", "metaslurp", "-threads", "2"]
