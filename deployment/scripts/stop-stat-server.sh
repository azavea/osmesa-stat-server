#!/bin/bash

if [ -z ${VERSION_TAG+x} ]; then
    echo "Do not run this script directly.  Use the Makefile in the parent directory."
    exit 1
fi

check_status() {
    STATUS=$(aws ecs describe-services --services osmesa-stats-server --cluster $ECS_CLUSTER | jq '.services[].status')
}

check_status
if [[ $STATUS == "\"ACTIVE\"" ]]; then
    aws ecs delete-service --service osmesa-stats-server --cluster $ECS_CLUSTER --force
    echo "Waiting for service to shut down"
    check_status
    while [[ $STATUS != "\"INACTIVE\"" ]]; do
        echo "  current status: $STATUS, still waiting"
        sleep 15s
        check_status
    done
else
    echo "Status was $STATUS, nothing to stop"
fi
