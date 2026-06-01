FROM eclipse-temurin:25-jdk-noble AS build

WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -q

COPY src/ src/
RUN ./mvnw package -Pprod -DskipTests -q

FROM eclipse-temurin:25-jre-noble

COPY --from=build /app/target/Kistogramm*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app.jar"]
