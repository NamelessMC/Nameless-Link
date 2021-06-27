#!/bin/bash
set -e
VERSION=2.0
./build.sh
docker build -t namelessmc/discord-link:$VERSION --build-arg LINK_VERSION=${VERSION} .
docker build -t namelessmc/discord-link-postgres:$VERSION postgres-docker
docker push namelessmc/discord-link:$VERSION
docker push namelessmc/discord-link-postgres:$VERSION
