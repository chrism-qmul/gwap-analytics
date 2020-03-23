FROM maven:3.6.3-jdk-11 AS build
COPY src /usr/src/app/src
COPY pom.xml /usr/src/app
RUN mvn -f /usr/src/app/pom.xml clean package

FROM openjdk:11-jre
COPY --from=build /usr/src/app/target/eventprocessor-0.1.jar /usr/app/app.jar 
ENTRYPOINT ["java","-jar","/usr/app/app.jar"] 
