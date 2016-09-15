[![Build Status](https://travis-ci.org/gentics/mesh.svg)](https://travis-ci.org/gentics/mesh)
[![Coverage Status](https://img.shields.io/coveralls/gentics/mesh.svg)](https://coveralls.io/r/gentics/mesh?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.gentics.mesh/mesh/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.gentics.mesh/mesh)
[![JavaDoc](https://javadoc-emblem.rhcloud.com/doc/com.gentics.mesh/mesh/badge.svg)](http://www.javadoc.io/doc/com.gentics.mesh/mesh)
[![License](http://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![Stack Overflow](http://img.shields.io/:stack%20overflow-genticsmesh-brightgreen.svg)](http://stackoverflow.com/questions/tagged/genticsmesh)

# Gentics Mesh

Gentics Mesh is an Open Source API-first CMS for developers. All contents can be stored/updated and retrieved using the [REST API](http://getmesh.io/docs/beta/raml/).

## Features

* Document level permissions
* Versioned content
* Webroot API for easy integration with modern routing frameworks
* Search API powered by elasticsearch
* Image API
* Tagging API
* Graph database at its core
* Docker support

![alt tag](http://getmesh.io/assets/mesh-heroimg.png)

### Download

* [Download from getmesh.io](http://getmesh.io/Download)
* [Docker Hub](https://hub.docker.com/r/gentics/mesh-demo/)
* [Maven Central](http://mvnrepository.com/artifact/com.gentics.mesh)

### Changelog

http://getmesh.io/docs/beta/changelog.html

### Documentation

* http://getmesh.io/docs/beta/
* http://getmesh.io/docs/beta/raml/


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
  "uuid" : "1f91269a4e6042c391269a4e6052c3e4",
  "creator" : {
    "name" : "admin",
    "uuid" : "2ac51bffa0b74843851bffa0b7784356"
  },
  "created" : 1473857597199,
  "editor" : {
    "name" : "admin",
    "uuid" : "2ac51bffa0b74843851bffa0b7784356"
  },
  "edited" : 1473857597200,
  "permissions" : [ "create", "read", "update", "delete" ],
  "language" : "en",
  "availableLanguages" : [ "en" ],
  "parentNode" : {
    "uuid" : "b253cd24d8b04b4993cd24d8b01b4927",
    "displayName" : "Automobiles",
    "schema" : {
      "name" : "category",
      "uuid" : "deae278d0c3a4303ae278d0c3a9303b6"
    }
  },
  "tags" : {
    "Colors" : {
      "uuid" : "ec9bd1d8fab044da9bd1d8fab0c4da51",
      "items" : [ {
        "name" : "Orange",
        "uuid" : "cbd9ced021d346a499ced021d306a4b0"
      } ]
    },
    "Fuels" : {
      "uuid" : "80904d30810b4f83904d30810b2f8336",
      "items" : [ {
        "name" : "Electricity",
        "uuid" : "8fcf087b56da49638f087b56da9963f9"
      } ]
    }
  },
  "childrenInfo" : { },
  "schema" : {
    "name" : "vehicle",
    "uuid" : "4fb140359c8c45f8b140359c8c35f88a",
    "version" : 1
  },
  "published" : false,
  "displayField" : "name",
  "fields" : {
    "price" : 101500,
    "stocklevel" : 20,
    "vehicleImage" : {
      "uuid" : "ecb339ba241d4f6fb339ba241dbf6f29",
      "creator" : {
        "name" : "admin",
        "uuid" : "2ac51bffa0b74843851bffa0b7784356"
      },
      "created" : 1473857596822,
      "editor" : {
        "name" : "admin",
        "uuid" : "2ac51bffa0b74843851bffa0b7784356"
      },
      "edited" : 1473857596822,
      "permissions" : [ "create", "read", "update", "delete" ],
      "language" : "en",
      "availableLanguages" : [ "en" ],
      "parentNode" : {
        "uuid" : "883abbd203374ccebabbd20337acce75",
        "displayName" : "Vehicle Images",
        "schema" : {
          "name" : "folder",
          "uuid" : "afefc9a0e2174396afc9a0e2175396e2"
        }
      },
      "tags" : { },
      "childrenInfo" : { },
      "schema" : {
        "name" : "vehicleImage",
        "uuid" : "3502fb222e77431882fb222e777318c3",
        "version" : 1
      },
      "published" : false,
      "displayField" : "name",
      "fields" : {
        "altText" : null,
        "image" : {
          "fileName" : "tesla-roadster.jpg",
          "sha512sum" : "2a56c85df60ab753f77fe75a63910b7e3f9ae89cd90e1906ad6210ee408ce07d5d95f269a21217ee045af8ac7d6c934324e49908d463971e31498b994b757d03",
          "fileSize" : 607113,
          "mimeType" : "image/jpeg",
          "type" : "binary"
        },
        "name" : "Tesla Roadster Image"
      },
      "breadcrumb" : [ {
        "uuid" : "883abbd203374ccebabbd20337acce75",
        "displayName" : "Vehicle Images"
      } ],
      "container" : false
    },
    "name" : "Tesla Roadster",
    "description" : "The Tesla Roadster is a battery electric vehicle (BEV) sports car produced by the electric car firm Tesla Motors in California between 2008 and 2012.",
    "weight" : 1305,
    "SKU" : 2
  },
  "breadcrumb" : [ {
    "uuid" : "b253cd24d8b04b4993cd24d8b01b4927",
    "displayName" : "Automobiles"
  } ],
  "container" : false
}
```



