#!/bin/bash

docker rm -f mongo
docker rm -f mongoexpress
docker-compose up -d
