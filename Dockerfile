FROM eclipse-temurin:21-jre

ARG JAR_FILE=target/Kistogramm*.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app.jar"]
