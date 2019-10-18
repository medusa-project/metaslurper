#!/bin/sh

if [ $# -lt 3 ]
then
    echo "Usage: docker-run-task.sh <env> <source key> <sink key>"
    exit 1
fi

source docker/env.sh env-common.list
source docker/env.sh env-$1.list

docker run -it \
    --env-file "docker/env-common.list" \
    --env-file "docker/env-$1.list" \
    $IMAGE_NAME \
    java -jar metaslurper.jar -source $2 -sink $3 -threads 2
