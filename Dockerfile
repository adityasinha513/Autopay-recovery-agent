FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

COPY src/ src/
RUN ./mvnw -B -DskipTests package \
    && mkdir -p /out \
    && cp target/autopay-recovery-0.0.1-SNAPSHOT.jar /out/app.jar

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
ENV SPRING_PROFILES_ACTIVE=prod

COPY --from=build --chown=10001:10001 /out/app.jar /app/app.jar

USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
