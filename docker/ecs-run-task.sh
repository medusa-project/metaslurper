#!/bin/sh
#
# Script for launching an ECS task using the aws command-line tool.
#
# Note that this is just a demo tool. In production, tasks would be launched
# by the web app using the ECS API.
#
# See: https://docs.aws.amazon.com/cli/latest/reference/ecs/run-task.html
#

source docker/env.sh

if [ -z "$1" ] || [ -z "$2" ];
  then
    echo "Source and sink service keys are required."
    exit
fi

aws ecs run-task \
    --launch-type FARGATE \
    --profile $AWS_PROFILE \
    --region $AWS_REGION \
    --cluster $ECS_CLUSTER \
    --task-definition $ECS_TASK_DEFINITION \
    --network-configuration "awsvpcConfiguration={subnets=[$ECS_SUBNET],securityGroups=[$ECS_SECURITY_GROUP],assignPublicIp=ENABLED}" \
    --overrides '{ "containerOverrides": [ { "name": "metaslurper", "command": [ "java", "-jar", "metaslurper.jar", "-source", "'$1'", "-sink", "'$2'", "-threads", "2" ] } ] }' \
