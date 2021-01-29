#
# N.B.: An application JAR file must be built before the image can be created.
# See the README.
#

FROM openjdk:15-slim

# The out-of-the-box JDK doesn't trust IDNC's SSL certificate. Download its
# root certificate and import it into the Java key store.
RUN echo | openssl s_client -servername idnc.library.illinois.edu -connect idnc.library.illinois.edu:443 \
    | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > idnc.crt \
    && keytool -noprompt -storepass changeit -import -alias idnc -keystore $JAVA_HOME/lib/security/cacerts -file idnc.crt \
    && rm idnc.crt

COPY ./target/metaslurper-1.0-SNAPSHOT.jar ./metaslurper.jar

# This will need to be overridden at runtime.
CMD ["java", "-jar", "metaslurper.jar", \
    "-source", "test", "-sink", "metaslurp", "-threads", "2"]
