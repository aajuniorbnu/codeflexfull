FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY bilheteria bilheteria
COPY pagamentos pagamentos
ARG SERVICE
RUN chmod +x mvnw && ./mvnw -q -pl ${SERVICE} -DskipTests package && cp ${SERVICE}/target/*.jar /app.jar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app.jar app.jar
ENV TZ=America/Sao_Paulo
EXPOSE 8080 8081
ENTRYPOINT ["java","-jar","app.jar"]
