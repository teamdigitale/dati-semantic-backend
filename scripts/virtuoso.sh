#!/bin/bash

set -e

docker >/dev/null 2>&1 || echo "Docker must be installed"

if [ "$#" -ne 1 ]; then
  echo "usage $0 ACTION - can be 'start' or 'stop'"
  exit 1
fi

if [ "$1" == "start" ]; then
    echo "starting virtuoso container"
    docker run --name my-virtuoso \
        -p 8890:8890 -p 1111:1111 \
        -e DBA_PASSWORD=myDbaPassword \
        -e SPARQL_UPDATE=true \
        -e DEFAULT_GRAPH=http://www.ndc.com/test-graph \
        -d tenforce/virtuoso
elif [ "$1" == "stop" ]; then
    echo "stopping virtuoso container"
    docker rm -f my-virtuoso
else
    echo "ERROR : unknown ACTION - can be 'start' or 'stop'"
    exit 1
fi
