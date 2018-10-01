export CONFIG_NAME := osm-stat-stream-config

# AWS properties
export CLUSTER_NAME := osm-stat-stream-cluster
export INSTANCE_TYPE := m4.xlarge
export KEYPAIR := nome-collab-azavea
export VPC := vpc-33497d57
export SUBNETS := [subnet-973597bd]
export SECURITY_GROUP := sg-6b227c23
export ECR_REPO := 501328983031.dkr.ecr.us-east-1.amazonaws.com/osm-stats-server:latest
export AWS_LOG_GROUP := osm-stats-server
export AWS_REGION := us-east-1

export HOST        := 0.0.0.0
export PORT        := 80

export DB_DRIVER   := s3://path/to/augdiffs/
export DB_URL      := https://planet.osm.org/replication/changeset/
export DB_USER     := [Start of Augdiff stream]
export DB_PASS     := [Start of changeset stream]

export TILE_BUCKET := [S3 bucket]
export TILE_PREFIX := [S3 prefix]
export TILE_SUFFIX := [Tile suffix (typically file extension)]
export GZIPPED     := [Whether to expect pre-gzipped tiles on S3 (true or false)]

export DB_URI := [URI to DB for writing outputs from stream]

