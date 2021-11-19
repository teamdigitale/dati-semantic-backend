#!/bin/bash

set -e

docker >/dev/null 2>&1 || echo "Docker must be installed"

docker run -p 9200:9200 \
  -e "discovery.type=single-node" \
  -d docker.elastic.co/elasticsearch/elasticsearch:7.12.0