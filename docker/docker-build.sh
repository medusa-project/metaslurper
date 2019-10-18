#!/bin/sh

if [ $# -lt 1 ]
then
    echo "Usage: docker-build.sh <env>"
    exit 1
fi

source docker/env.sh env-common.list
source docker/env.sh env-$1.list

docker build -f docker/Dockerfile -t $IMAGE_NAME .
