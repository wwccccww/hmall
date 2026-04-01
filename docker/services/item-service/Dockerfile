FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml ./
COPY hm-common/pom.xml hm-common/pom.xml
COPY item-service/pom.xml item-service/pom.xml
COPY hm-api/pom.xml hm-api/pom.xml
COPY hm-common/src hm-common/src
COPY item-service/src item-service/src
COPY hm-api/src hm-api/src
RUN mvn -q -pl item-service -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/item-service/target/item-service.jar /app/app.jar
ENV JAVA_OPTS=""
EXPOSE 8081
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]

