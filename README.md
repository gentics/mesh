[![License](http://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![Stack Overflow](http://img.shields.io/:stack%20overflow-genticsmesh-brightgreen.svg)](http://stackoverflow.com/questions/tagged/mesh-cms)

# Gentics Mesh

Gentics Mesh is an Open Source API-first CMS for developers. All contents can be stored/updated and retrieved using the [REST API](https://getmesh.io/docs/beta/raml/).

## Features

* Document level permissions
* Versioned content
* Webroot API for easy integration with modern routing frameworks
* Search API powered by elasticsearch
* GraphQL API
* Image API
* Tagging API
* Cluster support
* Graph database at its core
* Docker support

![alt tag](https://getmesh.io/assets/mesh-heroimg.png)

### Getting Started

You need to have at least JRE 8 of Java installed. See the [Getting Started page](https://getmesh.io/docs/beta/getting-started.html) for more information.

You can either download the all-in-one jar or fire-up one of the Docker images.

* [Download from getmesh.io](https://getmesh.io/Download)
* [Docker Hub](https://hub.docker.com/r/gentics/mesh/)
* [Maven](https://maven.gentics.com/maven2/com/gentics/mesh)

### [Changelog](https://getmesh.io/docs/beta/changelog.html)

### Documentation

* https://getmesh.io/docs/beta/
* https://getmesh.io/docs/beta/raml/

### UI

Gentics Mesh automatically ships with a UI which allows you to brows your contents.

The UI is can be accessed via http://localhost:8080/mesh-ui

### Typical usage

You can retrieved stored contents via the REST or GraphQL API.

First things first - You need to authenticate. Otherwise you will not be able to access your data.

* http://localhost:8080/api/v1/auth/login

You can post your credentials via JSON, use basic auth or send a JWT header - The choice is yours. If you open that URL in a browser you will most likly authenticate using basic auth.

#### REST API

Now that your are authenticated you can load content via the REST API.

Load a list of projects:

* http://localhost:8080/api/v1/projects

Or a list of contents

* http://localhost:8080/api/v1/demo/nodes

#### GraphQL

If you want to retrieve deeply nested data you may use the GraphiQL browser

* http://localhost:8080/api/v1/demo/graphql/browser/

Or try our [live demo](https://demo.getmesh.io/api/v1/demo/graphql/browser/).

### Example JSON

Typical Request/Response:

```
GET /api/v1/demo/nodes/1f91269a4e6042c391269a4e6052c3e4?lang=en,de HTTP/1.1
Host: localhost:8080
Connection: keep-alive
Accept: application/json, text/plain, */*


HTTP/1.1 200 OK
Content-Type: application/json; charset=utf-8
Cache-Control: no-cache
Content-Encoding: gzip
Transfer-Encoding: chunked

{
  "uuid" : "c7f284b8db9740fab284b8db97b0fa72",
  "creator" : {
    "uuid" : "344278e8bec74f6a8278e8bec76f6a87"
  },
  "created" : "2017-03-27T11:22:27Z",
  "editor" : {
    "uuid" : "344278e8bec74f6a8278e8bec76f6a87"
  },
  "edited" : "2017-03-27T11:22:35Z",
  "language" : "en",
  "availableLanguages" : [ "en" ],
  "parentNode" : {
    "projectName" : "demo",
    "uuid" : "3d77fe558cf743d3b7fe558cf783d343",
    "displayName" : "Vehicle Images",
    "schema" : {
      "name" : "folder",
      "uuid" : "35de83ec7df048d59e83ec7df028d50f"
    }
  },
  "tags" : [ ],
  "childrenInfo" : { },
  "schema" : {
    "name" : "vehicleImage",
    "uuid" : "4bae3a3ec02043abae3a3ec020d3ab42",
    "version" : 1
  },
  "displayField" : "name",
  "fields" : {
    "name" : "Tesla Roadster Image",
    "image" : {
      "fileName" : "tesla-roadster.jpg",
      "width" : 1024,
      "height" : 670,
      "sha512sum" : "2a56c85df60ab753f77fe75a63910b7e3f9ae89cd90e1906ad6210ee408ce07d5d95f269a21217ee045af8ac7d6c934324e49908d463971e31498b994b757d03",
      "fileSize" : 607113,
      "mimeType" : "image/jpeg",
      "dominantColor" : "#90786b"
    }
  },
  "breadcrumb" : [ {
    "projectName" : "demo",
    "uuid" : "3d77fe558cf743d3b7fe558cf783d343",
    "displayName" : "Vehicle Images",
    "schema" : {
      "name" : "folder",
      "uuid" : "35de83ec7df048d59e83ec7df028d50f"
    }
  } ],
  "version" : {
    "uuid" : "54d70c2d951d4188970c2d951d218875",
    "number" : "1.0"
  },
  "container" : false,
  "permissions" : {
    "create" : true,
    "read" : true,
    "update" : true,
    "delete" : true,
    "publish" : true,
    "readPublished" : true
  }
}
```

## IDE Setup - Eclipse

Make sure that you use at least Eclipse Neon.

Install the following maven m2e workshop plugins:

  * m2e-apt-plugin

Note: Make sure that your Eclipse Maven APT settings are set to "Automatically configure JDT APT". 
If you don't find this option you most likely need to install the M2E APT Plugin for eclipse.

Import all maven modules in your IDE.

Please note that this project is using Google Dagger for dependency injection. Adding new dependencies or beans may require a fresh build (via Project->Clean) of the mesh-core/mesh-api modules.
