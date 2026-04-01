FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml ./
COPY hm-common/pom.xml hm-common/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY hm-api/pom.xml hm-api/pom.xml
COPY hm-common/src hm-common/src
COPY user-service/src user-service/src
COPY hm-api/src hm-api/src
RUN mvn -q -pl user-service -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/user-service/target/user-service.jar /app/app.jar
ENV JAVA_OPTS=""
EXPOSE 8084
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]

