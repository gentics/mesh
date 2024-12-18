# Gentics Mesh Helm Chart


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
* Cluster write lock / sync writes is enabled. Thus write requests will be processed sequentially.
* Backup is disabled
* Request coordination is enabled. All requests will be redirected to the elected master.

## TODOs / Missing configuration

* Elasticsearch support
* Backup requires Gentics Mesh which includes Mesh CLI
* PV for image cache

## Changelog
### 1.3.16
* Change urls in chart for nexus migration

### 1.3.15
* Custom-Jobs now have a default `ttlSecondsAfterFinished` of 1 day to remove old jobs from cluster. This can be changed in the definition of your custom job in values.yaml
* The chart is adapted to the non-persisting images (Enterprise), removing unneeded configuration items.
* The backup/restore procedure can now choose the image parts to process: keycloak, uploads, database.

### 1.3.14
* Allow to configure mesh backup pod ressources with `.Values.backup.resources`

### 1.3.13
* Adding possiblity to set `vertxOptions.orderedBlockingHandlers` via values.yml

### 1.3.12
* Fix syntax Error with custom-podSecurityContext for custom-cronjobs

### 1.3.11
* `securityContext` value is now applied to backup-cronjob and custom-cronjobs.
* Allow to configure custom annotations for mesh pods with `podAnnotations`

### 1.3.10
* A new boolean config item `service.userHttp2` has been added, to allow connections over `h2c` protocol

### 1.3.9
* Add possibility to configure custom kubernetes cronjobs

### 1.3.8
* Add possibility to configure podDisruptionBudget

### 1.3.7
* Remove licenkey key insertion mechanism, as this is now natively supported by Mesh

### 1.3.6
* Fixes for licenseKey, also write when file is empty

### 1.3.5
* Deactivate OrientDB transaction commit timeout `graphTXCommitTimeout`

### 1.3.4
* Add licenseKey option to provide license key

### 1.3.3
* Add a service for each mesh instance to be reachable directly/independend (gentics-mesh-0, gentics-mesh-1, ....)

### 1.3.2
* Fix default configured binaryImageCache directory

### 1.3.1
* Changed image cache configuration
  * removed Values.persistence.sharedImages
  * added Values.persistance.imageCache
    * if enabled the generated resized images will be persisted on the shared volume. Make sure to account for the expected file sizes.

### 1.3.0
* Update deprecated APIs:
  * Kubernetes version 1.19.0 and above now uses `networking.k8s.io/v1`
  * Kubernetes version 1.21.0 and above now uses `batch/v1`

### 1.2.21
* Make previously hardcoded value MESH_GRAPH_TX_COMMIT_TIMEOUT configurable if clustering is used

### 1.2.20
* Make previously created service "mesh" configurable to use a custom name. So it's possible to run 2 instances in one namespace (`Values.service.mesh.name`)

### 1.2.19
* Added possibility to use backup without clustering. For this, the option `backup.withCluster` need to be set to false
* Removed backup-service and backup-cronjob when backup is disabled

### 1.2.18
* Added possibility to configure `failedJobsHistoryLimit` and `successfulJobsHistoryLimit` for backup cronjob in the values. The default value of `failedJobsHistoryLimit` has been increased from 1 -> 3. A plausibility check was introduced to check for a minimum backup-size

### 1.2.17
* Fixed issue that sometimes resulted in invalid YAML for the StatefulSet when the old and new livenessProbe were merged together. `livenessProbe` and `readinessProbe` in values will take precedence, otherwise default values with the updated livenessProbe will be used. 

### 1.2.16
* Default LogLevel can now be set via values (`logging.defaultLogLevel`)
* Custom `logback.xml` can now be injected via values (`logging.logback`). As default, the `config/common/logback.xml` file is used.
* Remove image configuration for backup pod. The backup pod will now use the same image as the main mesh instances

### 1.2.15
* PVC for backup deployment should now be created as intended
* Values.backup.resources can now be set for using different resources as the StatefulSet pods

### 1.2.14
* Bump to Gentics Mesh 1.6.21
* Uses new liveness check that was introduced in Gentics Mesh 1.6.20 by default
  * **Note**: If you use a Mesh version older than 1.6.20, the new healthchecks won't work and your instances will keep restarting. Either use a newer Mesh version or set the old livenessProbe to the previous endpoint.

### 1.2.13
* Volume for persisting cached images can now be created for each mesh replica by configuring Values.persistence.sharedImages
  * **Note**: Enabling this option requires uninstalling the current helm release of mesh first, because only certain keys of StatefulSets can be edited. Also make sure to have a current backup/snapshots of mesh in case the volumes get deleted when uninstalling.

### 1.2.12
* Changed clustering configuration defaults

### 1.2.11
* `MESH_PLUGIN_DIR` from extraEnv is filtered out for the backup deployment, override is possible via backup.pluginDir in values.yaml
* Upgraded to Gentics Mesh 1.6.18

### 1.2.10
* Allow config of `MESH_CLUSTER_TOPOLOGY_LOCK_TIMEOUT` / `topologyLockTimeout`
* Allow config of `MESH_CLUSTER_TOPOLOGY_CHANGE_READONLY` / `topologyChangeReadOnly`
* Upgraded to Gentics Mesh 1.6.13

### 1.2.9
* Minor fix for PodAntiAffinity

### 1.2.8
* Upgraded to Gentics Mesh 1.6.11
* Fix PodAntiAffinity configuration

### 1.2.7
* Add support for additional config maps via extraConfig property

### 1.2.6
* Add support for overriding `backup.sh` or `restore.sh`

### 1.2.5
* Add support for tar backups

### 1.2.4

* Fix backup pod not using possible configured pre-existing PVCs

### 1.2.3

* Add support for extraVolumes and extraVolumeMounts
* Add support for custom backup hostname

### 1.2.2

* Fixed issue template issue with shared PVC.

### 1.2.1

* It is now possible to configure a pre-existing `gentics-mesh-shared` PVC.

### 1.2.0

* Upgraded to Gentics Mesh 1.6.3
* Added backup and restore features to helm chart
* Added dedicated OrientDB Studio service and studio ingress. (Ingress disabled by default)
* The `gentics-mesh-shared` PVC will now also created when clustering is disabled

### 1.1.0

* When upgrading from version 1.0 to 1.1, please check your values.yaml: the `serviceType` moved from `service.*.type` to `service.type`.


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