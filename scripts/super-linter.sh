#!/bin/bash

set -e

docker >/dev/null 2>&1 || echo "Docker must be installed"

#move a directory up from this shell script
cd "$(dirname "${BASH_SOURCE[0]}")/.."

docker run \
  -e RUN_LOCAL=true \
  -e VALIDATE_MARKDOWN=false \
  -e IGNORE_GITIGNORED_FILES=true \
  -e VALIDATE_BASH=false \
  -e VALIDATE_JAVA=false \
  -e VALIDATE_GOOGLE_JAVA_FORMAT=false \
  -e VALIDATE_SQLFLUFF=false \
  --rm \
  -v "$(pwd)":/tmp/lint github/super-linter:slim-v4
