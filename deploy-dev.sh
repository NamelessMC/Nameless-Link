#!/bin/bash
set -e
VERSION=3
# ./build.sh
docker buildx build -t namelessmc/discord-link:dev --platform=linux/arm,linux/arm64,linux/amd64 --build-arg LINK_VERSION=${VERSION} . --push
