FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml ./
COPY hm-common/pom.xml hm-common/pom.xml
COPY hm-api/pom.xml hm-api/pom.xml
COPY ai-assistant-service/pom.xml ai-assistant-service/pom.xml
COPY hm-common/src hm-common/src
COPY hm-api/src hm-api/src
COPY ai-assistant-service/src ai-assistant-service/src
RUN mvn -q -pl ai-assistant-service -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/ai-assistant-service/target/ai-assistant-service.jar /app/app.jar
ENV JAVA_OPTS=""
EXPOSE 8090
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]

