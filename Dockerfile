# simple copy-jar runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ARG JAR_FILE=target/identity-access-service-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
ENV JAVA_OPTS=""
EXPOSE 8082
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]

