# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=local
EXPOSE 8082
COPY --from=build /app/target/*SNAPSHOT.jar /app/app.jar
USER 1001
ENTRYPOINT ["sh","-lc","java $JAVA_OPTS -jar /app/app.jar"
