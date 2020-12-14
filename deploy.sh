VERSION=1.0

docker build -t namelessmc/discord-link:$VERSION .
docker build -t namelessmc/discord-link-postgres:$VERSION postgres-docker
docker push namelessmc/discord-link:$VERSION
docker push namelessmc/discord-link-postgres:$VERSION
