#!/bin/bash

if [ -z ${VERSION_TAG+x} ]; then
    echo "No version tag has been set.  Do not run this script directly; instead, issue"
    echo "                              make build-container"
    echo "from the 'streaming' directory."
    exit 1
else
    echo "Version tag is set to '${VERSION_TAG}'"
fi

set -xe
SBT_DIR=$(pwd)/..
JAR_DIR=${SBT_DIR}/target/scala-2.11/
DOCKER_DIR=$(pwd)/docker

cd ${SBT_DIR}
./sbt clean assembly
cp ${JAR_DIR}/osm-stat-server.jar ${DOCKER_DIR}/osm-stat-server.jar

cd ${DOCKER_DIR}
docker build -f Dockerfile --tag osm_stat_server:${VERSION_TAG} .
