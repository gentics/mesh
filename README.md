[![License](http://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![Stack Overflow](http://img.shields.io/:stack%20overflow-genticsmesh-brightgreen.svg)](http://stackoverflow.com/questions/tagged/gentics-mesh)
[![Join the chat at https://gitter.im/gentics/mesh](https://badges.gitter.im/gentics/mesh.svg)](https://gitter.im/gentics/mesh?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=gentics_mesh&metric=alert_status)](https://sonarcloud.io/dashboard?id=gentics_mesh)
<a href="https://cla-assistant.io/gentics/mesh"><img src="https://cla-assistant.io/readme/badge/gentics/mesh" alt="CLA assistant" /></a>


# [Gentics Mesh](https://getmesh.io)

Gentics Mesh is an Open Source API-first CMS for developers. All contents can be stored/updated and retrieved using the [REST API](https://getmesh.io/docs/beta/raml/).

## Setup / Installation

### [Docker](https://getmesh.io/docs/administration-guide/#_run_with_docker)

```bash
docker run -p 8080:8080 gentics/mesh-demo:0.34.0
```

### [CLI](https://getmesh.io/docs/cli)

```bash
npm install mesh-cli -g
mesh docker start -t 0.34.0 -p 8080
```

### [Java](https://getmesh.io/docs/administration-guide/#_run_with_jar_file)

```bash
java -jar mesh-demo-0.34.0.jar
```

## Demo

### GraphQL API

* [GraphQL Example](https://demo.getmesh.io/api/v1/demo/graphql/browser/#query=query%20webroot(%24path%3A%20String)%20%7B%0A%20%20node(path%3A%20%24path)%20%7B%0A%20%20%20%20fields%20%7B%0A%20%20%20%20%20%20...%20on%20vehicle%20%7B%0A%20%20%20%20%20%20%20%20name%0A%20%20%20%20%20%20%20%20description%0A%20%20%20%20%20%20%20%20vehicleImage%20%7B%0A%20%20%20%20%20%20%20%20%20%20uuid%0A%20%20%20%20%20%20%20%20%20%20path%0A%20%20%20%20%20%20%20%20%20%20fields%20%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20...%20on%20vehicleImage%20%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20image%20%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20height%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20width%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20dominantColor%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%7D%0A%20%20%20%20%20%20%20%20%20%20%20%20%7D%0A%20%20%20%20%20%20%20%20%20%20%7D%0A%20%20%20%20%20%20%20%20%7D%0A%20%20%20%20%20%20%7D%0A%20%20%20%20%7D%0A%20%20%7D%0A%7D%0A)

```
query webroot($path: String) {
  node(path: $path) {
    fields {
      ... on vehicle {
        name
        description
        vehicleImage {
          uuid
          path
          fields {
            ... on vehicleImage {
              image {
                height
                width
                dominantColor
              }
            }
          }
        }
      }
    }
  }
}
---
{
  "path": "/yachts/indian-empress"
}
```

### [REST API](https://getmesh.io/docs/api/)

* List users [/users](https://demo.getmesh.io/api/v1/users)
* List nodes [/demo/nodes?perPage=5](https://demo.getmesh.io/api/v1/demo/nodes?perPage=5)
* Load by path [/demo/webroot/yachts/indian-empress](https://demo.getmesh.io/api/v1/demo/webroot/yachts/indian-empress)
* Load Image [/demo/webroot/images/yacht-pelorus.jpg?w=700](https://demo.getmesh.io/api/v1/demo/webroot/images/yacht-pelorus.jpg?w=700)

### UI

Gentics Mesh automatically ships with a UI which allows you to browse your contents.

https://demo.getmesh.io/mesh-ui

Login: admin/admin

### Example Frontend

https://demo.getmesh.io/demo

## Features

* GraphQL API
* Content events via websocket
* Document level permissions
* Versioned content
* Webroot API for easy integration with modern routing frameworks
* Search API powered by Elasticsearch
* Image API
* Tagging API
* Cluster support
* Monitoring support
* Graph database at its core
* Docker support
* Kubernetes support

![alt tag](https://getmesh.io/assets/mesh-heroimg.png)

## [Changelog](https://getmesh.io/docs/changelog)

## [Documentation](https://getmesh.io/docs)

## Typical usage

You can retrieve stored contents via the REST or GraphQL API.

First things first: you need to authenticate, otherwise you will not be able to access your data.

* http://localhost:8080/api/v1/auth/login

You can post your credentials via JSON, use basic auth or send a JWT header - the choice is yours. If you open that URL in a browser, you will most likely authenticate using basic auth.


## IDE Setup - Eclipse

Make sure that you use at least Eclipse Neon.

Install the following maven m2e workshop plugins:

  * m2e-apt-plugin

Note: Make sure that your Eclipse Maven APT settings are set to "Automatically configure JDT APT". 
If you don't find this option, you most likely need to install the M2E APT Plugin for eclipse.

Import all maven modules in your IDE.

Please note that this project is using Google Dagger for dependency injection. Adding new dependencies or beans may require a fresh build (via Project->Clean) of the mesh-core/mesh-api modules.
