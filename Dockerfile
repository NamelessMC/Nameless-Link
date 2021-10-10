FROM eclipse-temurin:16-focal

ARG LINK_VERSION=""
COPY target/Nameless-Link-${LINK_VERSION}.jar /app.jar

ENV WEBSERVER_PORT 80
ENV WEBSERVER_BIND 0.0.0.0
ENV XMX 128M

CMD ["sh", "-c", "java -jar -Xmx${XMX} /app.jar"]
