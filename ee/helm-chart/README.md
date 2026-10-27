# Gentics Mesh Helm Chart

## Introduction

This chart bootstraps a Gentics Mesh Enterprise installation

## Links

* [Installation Guide](https://www.gentics.com/cmp8/operations/kubernetes/installation)
* [Documentation](https://www.gentics.com/mesh/docs/getting-started/)
* [Changelog](https://www.gentics.com/cmp/changelog/)

## Update dependency charts

Set gentics-common `version` in `Chart.yml`
```bash
helm dependency update
```

## Building / Release

Helm chart is automatically built with a mesh-enterprise release