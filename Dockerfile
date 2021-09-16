FROM johnnyjayjay/leiningen:openjdk11 AS build
WORKDIR /usr/src/beepl
COPY . .
RUN lein uberjar

FROM openjdk:11
ARG jar=beepl-*-standalone.jar
WORKDIR /usr/app/beepl
COPY --from=build /usr/src/beepl/target/uberjar/$jar .
ENV jar=$jar
CMD java -jar $jar
