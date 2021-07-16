#!/bin/bash
set -e
VERSION=2.0
./build.sh
docker build -t namelessmc/discord-link:dev --build-arg LINK_VERSION=${VERSION} .
docker push namelessmc/discord-link:dev
