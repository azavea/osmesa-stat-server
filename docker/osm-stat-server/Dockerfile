FROM openjdk:9-jre

RUN \
  apt update \
  && apt install -y --no-install-recommends postgresql-client \
  && apt-get clean \
  && rm -rf /var/lib/apt/lists/*

COPY osm-stat-server.jar /opt/osm-stat-server.jar
COPY refresh-views.sh /usr/local/bin/refresh-views.sh

WORKDIR /opt