FROM eclipse-temurin:25-jre-noble

ARG JAR_FILE=target/Kistogramm*.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app.jar"]
