
clean:
	rm -rf docker/osm-stat-server/osm-stat-server.jar
	docker-compose down

docker/osm-stat-server/osm-stat-server.jar:
	sbt assembly
	mv target/scala-2.12/osm-stat-server.jar docker/osm-stat-server/osm-stat-server.jar

build: docker/osm-stat-server/osm-stat-server.jar
	docker-compose build

serve:
	docker-compose up

