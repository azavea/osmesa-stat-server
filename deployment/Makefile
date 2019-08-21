include config-deployment.mk

# If the user is on master branch, see if we should deploy to production
VERSION_TAG=$(shell ./scripts/get-tag.sh)
ifeq ($(VERSION_TAG), production)
	DATABASE=${PRODUCTION_DB}
	ECS_CLUSTER=${CLUSTER_NAME_DEPLOYMENT}
	TASK_SUFFIX=
else
	DATABASE=${STAGING_DB}
	ECS_CLUSTER=${CLUSTER_NAME_STAGING}
	TASK_SUFFIX=-staging
endif
DB_URI=${DB_BASE_URI}/${DATABASE}

.EXPORT_ALL_VARIABLES:

#############################
#  Docker image management  #
#############################

.PHONY: login-aws-registry tag-image push-image

build-container:
	./build-container.sh

login-aws-registry:
	eval `aws ecr get-login --no-include-email --region ${AWS_REGION}`

tag-image: build-container
	docker tag osm_stat_server:${VERSION_TAG} ${ECR_IMAGE}:${VERSION_TAG}

push-image: login-aws-registry tag-image
	docker push ${ECR_IMAGE}:${VERSION_TAG}

#######################
# Streaming AWS Tasks #
#######################

.PHONY: create-log-groups define-production-tasks define-staging-tasks deploy-stat-server

create-log-groups:
	./scripts/create-log-groups.sh

define-staging-tasks:
	./scripts/define-staging-tasks.sh

define-production-tasks:
	./scripts/define-production-tasks.sh

deploy-stat-server:
	aws ecs create-service \
	  --cluster "${ECS_CLUSTER}" \
	  --service-name "osmesa-stats-server" \
	  --task-definition "osmesa-stat-server${TASK_SUFFIX}" \
	  --desired-count 1 \
	  --launch-type FARGATE \
	  --scheduling-strategy REPLICA \
	  --network-configuration ${NETWORK_CONFIGURATION}
