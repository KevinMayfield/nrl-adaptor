FROM openjdk:11-slim
VOLUME /tmp

ADD target/nrl-adaptor.jar nrl-adaptor.jar

# ENV JAVA_OPTS="-Xms512m -Xmx1024m"

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/nrl-adaptor.jar"]

