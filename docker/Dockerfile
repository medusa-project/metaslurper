#
# N.B.: An application JAR file must be built before the image can be created.
# See the README.
#

FROM openjdk:15-slim

ENV PATH=$PATH:/opt/maven/bin

RUN apt-get update && apt-get install -y --no-install-recommends \
		wget

# Install Maven
RUN wget -q https://mirrors.ocf.berkeley.edu/apache/maven/maven-3/3.8.1/binaries/apache-maven-3.8.1-bin.tar.gz \
    && tar xfz apache-maven-3.8.1-bin.tar.gz \
    && mv apache-maven-3.8.1 /opt/maven \
    && rm apache-maven-3.8.1-bin.tar.gz

# The out-of-the-box JDK doesn't trust IDNC's SSL certificate. Download its
# root certificate and import it into the Java key store.
RUN echo | openssl s_client -servername idnc.library.illinois.edu -connect idnc.library.illinois.edu:443 \
    | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > idnc.crt \
    && keytool -noprompt -storepass changeit -import -alias idnc -keystore $JAVA_HOME/lib/security/cacerts -file idnc.crt \
    && rm idnc.crt

COPY ./pom.xml pom.xml
COPY ./src src

RUN mvn package -DskipTests \
    && mv target/metaslurper-*.jar ./metaslurper.jar

# This will need to be overridden at runtime.
CMD ["java", "-jar", "metaslurper.jar", \
    "-source", "test", "-sink", "metaslurp", "-threads", "2"]
