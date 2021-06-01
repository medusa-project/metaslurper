#!/bin/sh

if [ $# -lt 1 ]
then
    echo "Usage: redeploy.sh <env>"
    exit 1
fi

docker/docker-build.sh $1
docker/ecr-push.sh $1
