# Gentics Mesh 3.x Helm Chart


## Prerequisite

* Kubernetes
* Helm 3

## Setup

First the initial master database needs to be setup. This will automatically be handled by the helm chart when `replicaCount=1`.

```bash
helm install --wait -f example-values.yaml --set "replicaCount=1,backup.enabled=false" gentics-mesh .
```

Once the database is setup you can start the full stack by running.

```bash
helm upgrade --wait -f example-values.yaml gentics-mesh .
```

## Pull Secrets

```bash
kubectl create secret docker-registry docker-gentics-com \
--docker-server=docker.gentics.com  \
--docker-username=customer-xxx \
--docker-password=YOUR_PASSWORD
```

## Default Configuration

By default various features are enabled / settings:

* Server tokens will be omitted by default
* No update check will be performed
* Backup is disabled

## TODOs / Missing configuration

* Elasticsearch support
* Backup requires Gentics Mesh which includes Mesh CLI
* PV for image cache

## Changelog
### 3.0.1
* Initial Release of Mesh Helm Chart for Mesh 3.x

## Building / Release

Increase `version` in `Chart.yaml` and create git Tag. Then this tag can be pushed to Artifactory

### package current version 
```bash
helm package ./
```

### Upload the generated/packaged file (check filename)

```bash
curl -u USERNAME:PASSWORD -s -w "%{http_code}\n" https://repo.gentics.com/repository/helm/ --upload-file ./gentics-mesh-xxx.tgz
```
