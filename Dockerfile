FROM adoptopenjdk/openjdk11:alpine-slim

WORKDIR /app

COPY . .

EXPOSE 8080

RUN ./gradlew bootRun