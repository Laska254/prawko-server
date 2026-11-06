FROM eclipse-temurin:21-jre
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
USER 1000:1000
ENTRYPOINT ["java","-jar","/app.jar"]
