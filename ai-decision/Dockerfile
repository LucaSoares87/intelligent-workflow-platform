FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/ai-decision-0.1.0.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java","-jar","/app/app.jar"]
