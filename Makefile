VERSION=$(shell git rev-parse --short HEAD)

clean:
	rm -rf docker/osm-stat-server/osm-stat-server.jar
	rm -rf target
	docker-compose down

build: clean
	./sbt assembly
	ln target/scala-2.11/osm-stat-server.jar docker/osm-stat-server/osm-stat-server.jar
	docker-compose build

serve: build
	docker-compose up

publish: build
	docker tag quay.io/geotrellis/osm-stat-server:latest quay.io/geotrellis/osm-stat-server:${VERSION}
	docker push quay.io/geotrellis/osm-stat-server:latest
	docker push quay.io/geotrellis/osm-stat-server:${VERSION}
