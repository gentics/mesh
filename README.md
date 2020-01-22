# Mesh Commercial Plugins

This repository contains the sources for multiple Gentics Mesh Commercial plugins.

## CI

https://ci.gentics.com/view/Mesh-Plugins/job/mesh-commercial-plugins/

## Releasing

All commercial plugins are always released at once. Also, all released commercial plugins have the same version.

The current version can be found with `git describe --abbrev=0`

### Building with Jenkins
* https://ci.gentics.com/view/Mesh-Plugins/job/mesh-commercial-plugins/job/dev/build
* Click `Build with Parameters` on the left
* Check all checkboxes. Leave `meshVersion` empty.
* Wait until the build and release is done. If any tests fail, fix them and retry.

### Moving the artifacts from staging to release
* When the build is done, go to https://repo.apa-it.at/artifactory/webapp/#/artifacts/browse/tree/General
* Make sure you are logged in (top right, sign in with apa-sso using your apagtx... user)
* Right-click on `gtx-maven-releases-staging-mesh-plugins` in the tree view
* Choose `Move Content`
* Select `gtx-maven-releases-commerical` as the target repository
* Click `Move`

After that you should be able to download the released plugins (see below)

## Documentation

TBD

## Download

The released plugins can be downloaded from the official Maven Repository Site:

```https://maven.gentics.com/maven2-commercial/com/gentics/mesh/plugin/commercial/```

Or via `mvn`:

```bash
mvn dependency:get -Dartifact=com.gentics.mesh.plugin.commercial:mesh-keycloak-plugin:1.3.5 \
 -DremoteRepositories=gtx-commercial::default::https://maven.gentics.com/maven2-commercial \
 -Ddest=keycloak-plugin.jar -Dtransitive=false
```

Download of these plugins is restricted and requires an account on https://repo.apa-it.at. Customer accounts must be requested by Gentics Operations or via Mail to support@gentics.com.

Credentials for the repository have to be specified in the `.m2/settings.xml` see https://maven.gentics.com/ for more details.
