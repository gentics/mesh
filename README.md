# Mesh Commercial Plugins

This repository contains the sources for multiple Gentics Mesh Commercial plugins.

## CI

https://ci.gentics.com/view/Mesh-Plugins/job/mesh-commercial-plugins/

## Documentation

TBD

## Download

The released plugins can be downloaded from the official Maven Repository Site:

```https://maven.gentics.com/maven2-commercial/com/gentics/mesh/plugin/commercial/```

Or via `mvn`:

```bash
mvn dependency:get -Dartifact=com.gentics.mesh.plugin.commercial:mesh-keycloak-plugin:1.3.5 \
 -DremoteRepositories=gtx-commercial::default::https://maven.gentics.com/maven2-commercial \
 -Ddest=plugins -Dtransitive=false
```

Download of these plugins is restricted and requires an account on https://repo.apa-it.at. Customer accounts must be requested by Gentics Operations or via Mail to support@gentics.com.

Credentials for the repository have to be specified in the `.m2/settings.xml` see https://maven.gentics.com/ for more details.
