#!/bin/bash

if [ -z ${VERSION_TAG+x} ]; then
    echo "Do not run this script directly.  Use the Makefile in the parent directory."
    exit 1
fi

aws ecs register-task-definition \
    --family osmesa-stat-server \
    --task-role-arn "arn:aws:iam::${IAM_ACCOUNT}:role/ECSTaskS3" \
    --execution-role-arn "arn:aws:iam::${IAM_ACCOUNT}:role/ecsTaskExecutionRole" \
    --network-mode awsvpc \
    --requires-compatibilities EC2 FARGATE \
    --cpu "0.5 vCPU" \
    --memory "1 GB" \
    --container-definitions "[
	    {
	      \"logConfiguration\": {
	        \"logDriver\": \"awslogs\",
	        \"options\": {
	          \"awslogs-group\": \"/ecs/osmesa-stats-server${TASK_SUFFIX}\",
	          \"awslogs-region\": \"${AWS_REGION}\",
	          \"awslogs-stream-prefix\": \"ecs\"
	        }
	      },
	      \"command\": [
	        \"java\",
	        \"-jar\", \"/opt/osm-stat-server.jar\"
	      ],
	      \"environment\": [
	        {
	          \"name\": \"DATABASE_URL\",
	          \"value\": \"${DB_BASE_URI}/${PRODUCTION_DB}\"
	        },
	        {
	          \"name\": \"DB_DRIVER\",
	          \"value\": \"${DB_DRIVER}\"
	        },
	        {
	          \"name\": \"DB_URL\",
	          \"value\": \"${DB_JDBC_BASE_URL}/${PRODUCTION_DB}\"
	        },
	        {
	          \"name\": \"DB_USER\",
	          \"value\": \"${DB_USER}\"
	        },
	        {
	          \"name\": \"DB_PASS\",
	          \"value\": \"${DB_PASS}\"
	        },
	        {
                  \"name\": \"GZIPPED\",
                  \"value\": \"true\"
                },
                {
                  \"name\": \"HOST\",
                  \"value\": \"0.0.0.0\"
                },
                {
                  \"name\": \"PORT\",
                  \"value\": \"80\"
                },
                {
                  \"name\": \"TILE_BUCKET\",
	          \"value\": \"${TILE_BUCKET}\"
                },
                {
                  \"name\": \"TILE_PREFIX\",
	          \"value\": \"${TILE_PREFIX}\"
                }
	      ],
	      \"image\": \"${ECR_IMAGE}:production\",
	      \"name\": \"osmesa-stat-server\"
	    }
	  ]"

aws ecs register-task-definition \
    --family osmesa-stats-view-refresher \
    --task-role-arn "arn:aws:iam::${IAM_ACCOUNT}:role/ECSTaskS3" \
    --execution-role-arn "arn:aws:iam::${IAM_ACCOUNT}:role/ecsTaskExecutionRole" \
    --network-mode awsvpc \
    --requires-compatibilities EC2 FARGATE \
    --cpu "0.25 vCPU" \
    --memory "0.5 GB" \
    --container-definitions "[
            {
              \"logConfiguration\": {
                \"logDriver\": \"awslogs\",
                \"options\": {
	          \"awslogs-group\": \"/ecs/osmesa-stats-server${TASK_SUFFIX}\",
	          \"awslogs-region\": \"${AWS_REGION}\",
                  \"awslogs-stream-prefix\": \"ecs\"
                }
              },
              \"command\": [
                \"refresh-views.sh\"
              ],
              \"environment\": [
                {
                  \"name\": \"DATABASE_URL\",
	          \"value\": \"${DB_BASE_URI}/${PRODUCTION_DB}\"
                },
                {
                  \"name\": \"DATABASE_NAME\",
	          \"value\": \"${PRODUCTION_DB}\"
                }
              ],
	      \"image\": \"${ECR_IMAGE}:production\",
              \"name\": \"stats-view-refresher\"
            }
          ]"
