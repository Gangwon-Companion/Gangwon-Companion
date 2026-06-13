FROM gradle:8.14-jdk17 AS build
WORKDIR /workspace

COPY . .
RUN ./gradlew bootJar --no-daemon
RUN JAR_FILE=$(find build/libs -name '*.jar' ! -name '*-plain.jar' | head -n 1) && cp "$JAR_FILE" /app.jar

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
