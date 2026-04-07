FROM eclipse-temurin:21-jdk AS build

WORKDIR /app
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle/ gradle/
COPY shared/ shared/
COPY server/ server/

RUN ./gradlew :server:installDist --no-daemon -x test

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /app/server/build/install/server/ .

EXPOSE 8080
ENTRYPOINT ["bin/server"]
