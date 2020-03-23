FROM clojure:lein-2.8.1 as build
RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app
VOLUME /root/.m2
COPY project.clj /usr/src/app/
COPY . /usr/src/app
RUN lein deps && lein less4j once && mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" app-standalone.jar

FROM openjdk:8-jre-alpine
COPY --from=build /usr/src/app/app-standalone.jar /app-standalone.jar
COPY --from=build /usr/src/app/resources /resources
EXPOSE 3000
CMD ["java", "-jar", "/app-standalone.jar"]
