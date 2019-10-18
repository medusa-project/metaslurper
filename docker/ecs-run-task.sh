#!/bin/sh
#
# Script for launching an ECS task using the aws command-line tool.
#
# Note that this is just a demo tool. In production, tasks would be launched
# by the web app using the ECS API.
#
# See: https://docs.aws.amazon.com/cli/latest/reference/ecs/run-task.html
#

if [ $# -lt 2 ]
then
    echo "Usage: ecs-run-task.sh <env> <source key> <sink key>"
    exit 1
fi

source docker/env.sh env-common.list
source docker/env.sh env-$1.list

aws ecs run-task \
    --launch-type FARGATE \
    --profile $AWS_PROFILE \
    --region $AWS_REGION \
    --cluster $ECS_CLUSTER \
    --count 1 \
    --task-definition $ECS_TASK_DEFINITION \
    --network-configuration "awsvpcConfiguration={subnets=[$ECS_SUBNET],securityGroups=[$ECS_SECURITY_GROUP],assignPublicIp=ENABLED}" \
    --overrides '{ "containerOverrides": [ { "name": "metaslurper", "command": [ "java", "-jar", "metaslurper.jar", "-source", "'$2'", "-sink", "'$3'", "-threads", "2" ] } ] }' \
