#!/bin/sh
#
# Builds a Docker image and pushes it to AWS ECR.
#

if [ $# -lt 1 ]
then
    echo "Usage: ecr-push.sh <env>"
    exit 1
fi

source docker/env.sh env-common.list
source docker/env.sh env-$1.list

aws ecr get-login-password --region $AWS_REGION --profile $AWS_PROFILE \
    | docker login --username AWS --password-stdin $ECR_HOST
docker tag $DOCKER_TAG $ECR_HOST/$DOCKER_TAG
docker push $ECR_HOST/$DOCKER_TAG
