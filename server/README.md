## What is Gentics Mesh?

Gentics Mesh is a API-Centric CMS Server written in Java which enables you to store and retrieve JSON documents and binaries via REST. Gentics Mesh will also provide you a lot of powerful tools build around and for those documents.

* Document level permission system
* Search API
* Webroot API for easy integration with modern routing frameworks
* Image Manipulation API
* Tagging system
* Schema system for documents
* Clustering (in development)
* Versioning of your documents

## How to use the image?

```
docker run --rm --name mesh -p 8080:8080 -e MESH_ADMIN_PASSWORD=finger gentics/mesh
```

## Environment

* MESH_ADMIN_PASSWORD - Admin password used during initial setup

## Volumes

*  /data - Data folder for database, uploads, search index
