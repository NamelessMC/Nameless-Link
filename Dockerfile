FROM adoptopenjdk/openjdk15-openj9:alpine

COPY target/Nameless-Link.jar /app.jar

ENV WEBSERVER_PORT 80

CMD ["java", "-jar", "/app.jar"]
