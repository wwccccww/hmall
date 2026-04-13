FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml ./
COPY hm-common/pom.xml hm-common/pom.xml
COPY item-service/pom.xml item-service/pom.xml
COPY cart-service/pom.xml cart-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY hm-api/pom.xml hm-api/pom.xml
COPY trade-service/pom.xml trade-service/pom.xml
COPY pay-service/pom.xml pay-service/pom.xml
COPY hm-gateway/pom.xml hm-gateway/pom.xml
COPY promotion-service/pom.xml promotion-service/pom.xml
COPY ai-assistant-service/pom.xml ai-assistant-service/pom.xml
COPY hm-common/src hm-common/src
COPY trade-service/src trade-service/src
COPY hm-api/src hm-api/src
RUN mvn -q -pl trade-service -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/trade-service/target/hm-trade.jar /app/app.jar
# Seata 依赖旧版 CGLIB，JDK17+ 需放开 java.lang 反射，否则 GlobalTransactionScanner 初始化报 InaccessibleObjectException
ENV JAVA_OPTS="--add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED"
EXPOSE 8085
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
