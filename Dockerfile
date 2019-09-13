FROM openjdk:11-slim
VOLUME /tmp

COPY target/nrl-adaptor.jar nrl-adaptor.jar

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/nrl-adaptor.jar"]

