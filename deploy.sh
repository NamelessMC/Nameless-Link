#!/bin/bash
set -e
VERSION=2.0
./build.sh
export DOCKER_CLI_EXPERIMENTAL=enabled
docker buildx build -t namelessmc/discord-link:$VERSION --platform=linux/arm,linux/arm64,linux/amd64 --build-arg LINK_VERSION=${VERSION} . --push
docker buildx build -t namelessmc/discord-link-postgres:$VERSION --platform=linux/arm,linux/arm64,linux/amd64 postgres-docker --push
