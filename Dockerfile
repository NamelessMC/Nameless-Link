FROM eclipse-temurin:17

COPY target/Nameless-Link.jar /app.jar

ENV WEBSERVER_PORT 80
ENV WEBSERVER_BIND 0.0.0.0
ENV JAVA_TOOL_OPTIONS -Xmx256M

CMD ["java", "-jar", "/app.jar"]
