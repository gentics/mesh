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

After that you should be able to download the released plugins (see below)

## Documentation

TBD

## Download

The released plugins can be downloaded from the official Maven Repository Site:

```https://repo.gentics.com/```

Or via `mvn`:

```bash
mvn dependency:get -Dartifact=com.gentics.mesh.plugin.commercial:mesh-keycloak-plugin:1.3.5 \
 -DremoteRepositories=gentics.nexus.snapshots::default::https://repo.gentics.com/repository/maven-releases/ \
 -Ddest=keycloak-plugin.jar -Dtransitive=false
```

Download of these plugins is restricted and requires an account on https://repo.gentics.com. Customer accounts must be requested by Gentics Operations or via Mail to support@gentics.com.

Credentials for the repository have to be specified in the `.m2/settings.xml` see https://maven.gentics.com/ for more details.
