version: '3.0'
services:
  stats-server:
    image: ${ECR_REPO}:latest
    command: java -jar /opt/osm-stat-server.jar
    ports:
      - ${PORT}:${PORT}
    environment:
      - HOST=${HOST}
      - PORT=${PORT}
      - DB_DRIVER=${DB_DRIVER}
      - DB_URL=${DB_URL}
      - DB_USER=${DB_USER}
      - DB_PASS=${DB_PASS}
      - TILE_BUCKET=${TILE_BUCKET}
      - TILE_PREFIX=${TILE_PREFIX}
      - GZIPPED=${GZIPPED}
      - DATABASE_URL=${DATABASE_URL}
    logging:
      driver: awslogs
      options:
        awslogs-group: ${AWS_LOG_GROUP}
        awslogs-region: ${AWS_REGION}
        awslogs-stream-prefix: osmesa-stat-server
