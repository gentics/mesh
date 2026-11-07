<h1 align="center">Gentics Mesh</h1>
<h3 align="center">The headless content repository of Gentics CMP – API-first, permission-aware, AI-ready</h3>

<p align="center">
  <a href="LICENSING.md"><img src="https://img.shields.io/badge/core-Apache--2.0-brightgreen.svg" alt="Core: Apache-2.0" /></a>
  <a href="LICENSING.md"><img src="https://img.shields.io/badge/ee-BUSL--1.1-blue.svg" alt="Enterprise Modules: BUSL-1.1" /></a>
  <a href="https://hub.docker.com/r/gentics/mesh"><img src="https://img.shields.io/badge/docker-gentics%2Fmesh-blue.svg" alt="Docker" /></a>
  <a href="https://github.com/gentics/mesh/releases"><img src="https://img.shields.io/github/v/release/gentics/mesh?sort=semver" alt="Latest release" /></a>
  <a href="https://stackoverflow.com/questions/tagged/gentics-mesh"><img src="https://img.shields.io/badge/stack%20overflow-gentics--mesh-brightgreen.svg" alt="Stack Overflow" /></a>
  <a href="https://cla-assistant.io/gentics/mesh"><img src="https://cla-assistant.io/readme/badge/gentics/mesh" alt="CLA assistant" /></a>
</p>

Gentics Mesh holds your content and hands it out through REST and GraphQL, with permissions, versioning,
multi-language support, search and events built in. You bring the frontend: a portal, a mobile app, a
static site generator, a digital signage screen or an AI agent. In Gentics CMP it is the hub between
[Gentics CMS](https://github.com/gentics/cms-oss), which publishes into it, and the
[portals](https://github.com/gentics/cmp-portals), which render from it. It also runs perfectly well on its
own.

## Where it sits in Gentics CMP

| Repository | Component |
| --- | --- |
| [gentics/cms-oss](https://github.com/gentics/cms-oss) | Gentics CMS – editing, publishing, Workspace, MCP tools |
| [gentics/mesh](https://github.com/gentics/mesh) | **This repo.** Gentics Mesh – headless content repository, search, Content RAG, MCP server |
| [gentics/cmp-portals](https://github.com/gentics/cmp-portals) | Portal \| php and Portal \| java – render content from Mesh |
| [gentics/cmp](https://github.com/gentics/cmp) | Umbrella: platform changelog, guides, Helm chart, licensing overview |

The [cmp README](https://github.com/gentics/cmp#readme) has the big picture and the cross-repository
getting-started guide.

## What you get

* [GraphQL API](https://www.gentics.com/mesh/docs/graphql/) and a full REST API, including a
  [webroot API](https://www.gentics.com/mesh/docs/features/#webroot) that maps URL paths to content for
  modern routing frameworks
* [Users, roles and document-level permissions](https://www.gentics.com/mesh/docs/features/#_permissions)
  that apply to every API, every search and every AI tool
* [Versioned content](https://www.gentics.com/mesh/docs/features/#versioning), multi-language fields,
  [tagging](https://www.gentics.com/mesh/docs/building-blocks/#_tag)
* [Search powered by Elasticsearch](https://www.gentics.com/mesh/docs/elasticsearch/) and an
  [image manipulation API](https://www.gentics.com/mesh/docs/features/#imagemanipulation)
* [Content events via websocket](https://www.gentics.com/mesh/docs/events/),
  [clustering](https://www.gentics.com/mesh/docs/clustering/) and
  [monitoring](https://www.gentics.com/mesh/docs/monitoring/)
* **MCP server** ([`mcp-server`](mcp-server)): the Model Context Protocol adapter for Mesh, with stdio
  transport for Claude Desktop, Cursor and friends and HTTP transport for agent frameworks; same tools on
  both, same permissions as the REST API
* **Content RAG** ([`content-rag`](content-rag)): permission-filtered semantic search and
  source-cited answers over Mesh content, indexed by the standard Mesh sync into Elasticsearch

## Repository map

```
mesh/
├── api, rest-model, rest-client   public API, REST models, Java client
├── core                           the content repository: schemas, nodes, permissions, versioning
├── services, verticles, server    the server (Docker image gentics/mesh)
├── elasticsearch                  search integration
├── distributed, distributed-coordinator   clustering
├── mcp-server                     MCP server (stdio + HTTP)
├── content-rag                    RAG index handler, chunking, rag-service
├── connectors                     database connectors (core)
├── plugin-api, plugin-*, plugins  plugin system and the open-source plugins
├── doc, changelog, bom
├── tests, database-tests
└── ee/                            Enterprise Modules (BUSL-1.1)
    ├── connectors/                mssql, oracle, postgres, common-enterprise
    ├── plugins/                   keycloak-plugin, search-plugin, favorite-plugin
    └── helm-chart                 enterprise Helm chart
```

## Getting started

### Run it

```bash
docker run -p 8080:8080 gentics/mesh:latest
```

Then log in via the API as shown below. The
[Getting Started Guide](https://www.gentics.com/mesh/docs/getting-started/) explains the building blocks
(projects, schemas, nodes, branches), the
[Administration Guide](https://www.gentics.com/mesh/docs/administration-guide/) covers databases, clustering
and production setups. For Kubernetes there is the Helm chart in [`ee/helm-chart`](ee/helm-chart) and the
[Gentics CMP umbrella chart](https://github.com/gentics/cmp/tree/master/ee/cmp-chart).

### Talk to it

Authenticate first – JSON body, basic auth or a JWT header, your choice:

```bash
curl -X POST http://localhost:8080/api/v2/auth/login \
     -H 'Content-Type: application/json' \
     -d '{"username":"<user>","password":"<password>"}'
```

Then fetch content with GraphQL (the `IntrospectionQuery` is supported, so any GraphQL-aware REST tool
gives you code assistance):

```graphql
query webroot($path: String) {
  node(path: $path) {
    ... on vehicle {
      fields {
        name
        description
        vehicleImage {
          uuid
          path
          ... on vehicleImage {
            fields { image { height width dominantColor } }
          }
        }
      }
    }
  }
}
```

The full [REST API reference](https://www.gentics.com/mesh/docs/api/) and the
[GraphQL documentation](https://www.gentics.com/mesh/docs/graphql/) are in the Mesh docs.

### Build it

Requirements: Java 17, Maven 3.9+, Docker (for the tests).

```bash
git clone https://github.com/gentics/mesh.git
cd mesh
mvn clean install -DskipTests
```

The reactor builds the core, the MCP server, Content RAG and the `ee/` modules. The
[Contributing Guidelines](https://www.gentics.com/mesh/docs/contributing/) explain the IDE setup and the
codebase; [`mcp-server/README.md`](mcp-server/README.md) and [`content-rag/README.md`](content-rag/README.md)
have the details of those two modules.

### Branches

* `dev` – next release
* `hotfix-<version>` – maintenance of released versions
* `f-<ticket>` – feature branches

## Documentation and help

* [Gentics Mesh documentation](https://www.gentics.com/mesh/docs/)
* [Gentics CMP changelog](https://www.gentics.com/cmp/changelog/) and [guides](https://www.gentics.com/guides/) across all components
* Questions: [Stack Overflow](https://stackoverflow.com/questions/tagged/gentics-mesh) or the
  [gitter channel](https://gitter.im/gentics/mesh)
* Issues: [github.com/gentics/mesh/issues](https://github.com/gentics/mesh/issues)

## Licensing

Gentics Mesh is **open core**:

* **Core – Apache License 2.0.** Everything outside `ee/`. OSI-approved open source: run it in production,
  modify it, redistribute it.
* **Enterprise Modules – Business Source License 1.1.** Everything inside `ee/`: the MS SQL, Oracle and PostgreSQL connectors, the Keycloak, search and favorite plugins and the enterprise Helm chart.
  The source is public and free to use for development, testing, evaluation and demonstration. Production
  use requires a commercial license – contact [sales@gentics.com](mailto:sales@gentics.com). Every module
  converts to Apache 2.0 three years after its release (Change Date); the exact dates are in
  [`ee/LICENSE`](ee/LICENSE).

The core runs without `ee/`. [`LICENSING.md`](LICENSING.md) has the details, the
[cmp repository](https://github.com/gentics/cmp/blob/master/LICENSING.md) the overview across the platform.

## Contributing

Pull requests are welcome.

* Contributions to the core are accepted under the Apache License 2.0 with a DCO sign-off (`git commit -s`).
* Contributions to `ee/` additionally need our [Contributor License Agreement](CLA-individual.md)
  ([corporate version](CLA-corporate.md)). The CLA bot asks on your first pull request that touches `ee/`.

See [`CONTRIBUTING.md`](CONTRIBUTING.md). Found a bug? Open an issue in this repository.

## Commercial services

Gentics Software GmbH, the company behind Gentics Mesh, offers product maintenance, developer support,
consulting and custom development. Contact [sales@gentics.com](mailto:sales@gentics.com).

---

Copyright (c) 2014-2026 Gentics Software GmbH. Gentics is a registered trademark of Gentics Software GmbH,
see [`TRADEMARKS.md`](TRADEMARKS.md).
