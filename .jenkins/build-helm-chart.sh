#!/bin/bash

set -o nounset
set -o errexit

VERSION=${1}
REPO_USERNAME=${REPO_USERNAME:-}
REPO_PASSWORD=${REPO_PASSWORD:-}

echo "Packaging Helm Chart gentics/gentics-mesh with Version $VERSION"

docker run --rm -v $(pwd):/apps -w /apps \
    --pids-limit 4096 \
    -e REPO_USERNAME="${REPO_USERNAME}" -e REPO_PASSWORD="${REPO_PASSWORD}" \
    -e VERSION="${VERSION}" \
    -e GOMAXPROCS=2 \
    --entrypoint sh \
    alpine/helm:3.11.1 -c '
        helm repo add --username "$REPO_USERNAME" --password "$REPO_PASSWORD" gentics https://repo.gentics.com/repository/helm/
        helm repo add --username "$REPO_USERNAME" --password "$REPO_PASSWORD" gentics-snapshots https://repo.gentics.com/repository/helm-snapshots/
        helm repo update
        helm dependency build ee/helm-chart
        helm package ee/helm-chart --version $VERSION --app-version $VERSION -d target
    '