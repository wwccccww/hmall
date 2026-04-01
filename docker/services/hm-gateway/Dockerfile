FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml ./
COPY hm-common/pom.xml hm-common/pom.xml
COPY hm-gateway/pom.xml hm-gateway/pom.xml
COPY hm-api/pom.xml hm-api/pom.xml
COPY hm-common/src hm-common/src
COPY hm-gateway/src hm-gateway/src
COPY hm-api/src hm-api/src
RUN mvn -q -pl hm-gateway -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/hm-gateway/target/hm-gateway.jar /app/app.jar
ENV JAVA_OPTS=""
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]

