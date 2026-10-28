#!/bin/bash



docker run --rm -p 8080:8080 -e MESH_ELASTICSEARCH_URL=null \
	-e MESH_ELASTICSEARCH_START_EMBEDDED=false \
	-e MESH_GRAPH_DB_DIRECTORY=null \
	-v $PWD/target/it:/plugins \
	gentics/mesh:0.41.0
