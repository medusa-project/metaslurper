#!/bin/sh

if [ -z "$1" ] || [ -z "$2" ];
  then
    echo "Source and sink service keys are required."
    exit
fi

source docker/env.sh

docker run -it \
    --env-file docker/env.list \
    $APP_NAME \
    java -jar metaslurper.jar -source $1 -sink $2 -threads 2
