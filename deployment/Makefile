include config-aws.mk  # Variables for AWS options

# The osmesa container
LOCAL_IMG := quay.io/geotrellis/osm-stat-server:latest


#########
#  AWS  #
#########

login-aws-registry:
	eval `aws ecr get-login --no-include-email --region us-east-1`

tag-image: login-aws-registry
	docker tag ${LOCAL_IMG} ${ECR_REPO}

push-image: login-aws-registry tag-image
	docker push ${ECR_REPO}

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
	  --region us-east-1 \
	  --config-name ${CONFIG_NAME}

cluster-up:
	ecs-cli up \
	  --keypair ${KEYPAIR} \
	  --instance-role osmstats-deploy_ec2 \
	  --size 1 \
	  --instance-type ${INSTANCE_TYPE} \
	  --cluster-config ${CONFIG_NAME} \
	  --subnets ${SUBNETS} \
	  --vpc ${VPC} \
	  --force \
	  --verbose

cluster-down:
	ecs-cli down --cluster-config ${CONFIG_NAME}

create-service:
	ecs-cli compose \
	  --file docker-compose.deploy.yml create \
	  --cluster ${CLUSTER_NAME}

start-service: create-service
	ecs-cli compose --file docker-compose.deploy.yml service up \
	  --create-log-groups \
	  --cluster osm-stat-stream-cluster

stop-service:
	ecs-cli compose down


#########
#  ALL  #
#########
build-container:
	cd .. && ./build-container.sh

clean:
	rm -f docker-compose.deploy.yml

