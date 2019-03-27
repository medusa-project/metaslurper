#!/bin/sh

mvn clean package -DskipTests
docker/docker-build.sh
docker/ecr-push.sh
