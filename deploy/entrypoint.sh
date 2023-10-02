#!/bin/sh

envsubst < client.properties.template > client.properties
java -jar /app/simple-streaming-app-all.jar
