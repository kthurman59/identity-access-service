# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN ./mvnw -B -DskipTests package && \
    JAR="$(ls target/*.jar | head -n 1)" && \
    cp "$JAR" /app/app.jar

FROM eclipse-temurin:21-jre
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
WORKDIR /app
COPY --from=build /app/app.jar /app/app.jar
RUN useradd --system --create-home --uid 10001 appuser
USER appuser
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app/app.jar"]

