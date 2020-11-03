FROM adoptopenjdk/openjdk15-openj9:alpine

COPY target/Nameless-Link.jar /app.jar

CMD ["java", "-jar", "/app.jar"]
