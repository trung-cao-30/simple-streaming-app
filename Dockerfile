# Use the official Gradle image to build our application
FROM gradle:8.1-jdk11 as build

# Set the working directory
WORKDIR /home/gradle/app

# Copy the build files first to make use of Docker's caching and speed up builds when your source changes
COPY *.gradle ./
COPY client.properties.template ./
COPY deploy/entrypoint.sh ./

# Copy the source code
COPY src ./src

# Build the application
RUN gradle clean build -x test

# Use the official OpenJDK base image for runtime
FROM openjdk:11-jre-slim

# Copy the built application JAR from the build image
COPY --from=build /home/gradle/app/build/libs/simple-streaming-app-all.jar /app/simple-streaming-app-all.jar
COPY --from=build /home/gradle/app/client.properties.template ./
COPY --from=build /home/gradle/app/entrypoint.sh ./

RUN chmod +x entrypoint.sh

# healthcheck port at /health
EXPOSE 8000

# to have envsubst
RUN apt-get update && apt-get install gettext-base

# Command to run the application
CMD ["/entrypoint.sh"]
