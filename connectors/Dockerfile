FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/connectors-0.1.0.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java","-jar","/app/app.jar"]
