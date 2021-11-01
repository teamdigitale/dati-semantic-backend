FROM adoptopenjdk/openjdk11:jre-11.0.6_10-alpine

WORKDIR /app

COPY . .

RUN "./gradlew bootRun"
