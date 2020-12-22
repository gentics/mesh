# Gentics Mesh - MADL Parent

This module is the parent pom for various other MADL modules.

## What is MADL?

MADL stands for `Mesh Abstraction Database Layer` which aims to replace Ferma.

## Goals / Background

The main goal for MADL was to replace the Ferma library. MADL also provides a dedicated index and type handling API which is already in use.
The ferma / tinkerpop library did not provide such a feature since GraphDBs often have vendor specific APIs to handle types and indices.
