FROM adoptopenjdk/openjdk15-openj9

COPY target/Nameless-Link.jar /app.jar

ENV WEBSERVER_PORT 80
ENV XMX 128M

CMD ["sh", "-c", "java -jar -Xmx${XMX} /app.jar"]
