#!/bin/sh

./mvnw -f smoke-tests/pom.xml clean package && \
  docker-compose -p monitoring up --build -d