include config-aws.mk  # Variables for AWS options

# The osmesa container
LOCAL_IMG := quay.io/geotrellis/osm-stat-server:latest


#########
#  AWS  #
#########

login-aws-registry:
	eval `aws ecr get-login --no-include-email --region ${AWS_REGION}`

tag-image:
	docker tag ${LOCAL_IMG} ${ECR_REPO}

push-image: login-aws-registry tag-image
	docker push ${ECR_REPO}

.PHONY: docker-compose.deploy.yml

docker-compose.deploy.yml:
	export ECR_REPO=${ECR_REPO}
	export AWS_LOG_GROUP=${AWS_LOG_GROUP}; \
	export AWS_REGION=${AWS_REGION}; \
	export HOST=${HOST}; \
	export PORT=${PORT}; \
	export DB_DRIVER=${DB_DRIVER}; \
	export DB_URL=${DB_URL}; \
	export DB_USER=${DB_USER}; \
	export DB_PASS=${DB_PASS}; \
	export TILE_BUCKET=${TILE_BUCKET}; \
	export TILE_PREFIX=${TILE_PREFIX}; \
	export GZIPPED=${GZIPPED}; \
		./expand.sh docker-compose.deploy.yml.tpl > docker-compose.deploy.yml

configure-cluster:
	ecs-cli configure \
	  --cluster ${CLUSTER_NAME} \
	  --region ${AWS_REGION} \
	  --config-name ${CONFIG_NAME}

cluster-up:
	ecs-cli up \
	  --keypair ${KEYPAIR} \
	  --instance-role ${INSTANCE_ROLE} \
	  --size 1 \
	  --instance-type ${INSTANCE_TYPE} \
	  --cluster-config ${CONFIG_NAME} \
	  --subnets ${SUBNETS} \
	  --vpc ${VPC} \
	  --force \
	  --verbose

cluster-down:
	ecs-cli down --cluster-config ${CONFIG_NAME}

.PHONY: create-service

create-service: docker-compose.deploy.yml configure-cluster
	ecs-cli compose \
	  --file $< create \
	  --cluster ${CLUSTER_NAME}

start-service: docker-compose.deploy.yml configure-cluster create-service
	ecs-cli compose --file $< service up \
	  --create-log-groups \
	  --cluster ${CLUSTER_NAME}

stop-service:
	ecs-cli compose down


#########
#  ALL  #
#########
build-container:
	make -C .. build

clean:
	rm -f docker-compose.deploy.yml

