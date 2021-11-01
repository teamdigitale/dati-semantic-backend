FROM adoptopenjdk/openjdk11:alpine-slim

WORKDIR /app

COPY . .

RUN ./gradlew bootRun
