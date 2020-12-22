<h1 align="center">
  <a href="https://getmesh.io">
    <img src="https://getmesh.io/assets/gentics-mesh-logo.png" width="420" alt="Gentics Mesh" />
  </a>
</h1>

<h3 align="center">Gentics Mesh is your friendly, enterprise-grade, open-source headless CMS</h3>

<p align="center">
To be honest, it’s more than that: it’s your application development platform
to develop your websites, your IoT applications, your mobile apps, your smart
devices and your digital signage solutions. With its best-in-class APIs, a
complete feature list and great documentation, you’ll get your projects done
successfully in less time, no matter which technology you prefer.
</p>

<br />

<p align="center">

 <img src="https://img.shields.io/badge/status-stable-brightgreen.svg" alt="stable" />

 <a href="https://www.apache.org/licenses/LICENSE-2.0">
  <img src="https://img.shields.io/:license-apache-brightgreen.svg" alt="License" />
 </a>
 <a href="https://stackoverflow.com/questions/tagged/gentics-mesh">
  <img src="https://img.shields.io/badge/stack%20overflow-gentics--mesh-brightgreen.svg" alt="Stack Overflow" />
 </a>
 <a href="https://gitter.im/gentics/mesh?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge">
  <img src="https://badges.gitter.im/gentics/mesh.svg" alt="Join the chat at https://gitter.im/gentics/mesh" />
 </a>
 <a href="https://github.com/gentics/mesh/releases">
  <img src="https://img.shields.io/github/v/release/gentics/mesh?sort=semver" alt="Latest release" />
 </a>
 <a href="https://sonarcloud.io/dashboard?id=gentics_mesh">
  <img src="https://sonarcloud.io/api/project_badges/measure?project=gentics_mesh&metric=alert_status" alt="Quality Gate Status" />
 </a>
 <a href="https://cla-assistant.io/gentics/mesh">
  <img src="https://cla-assistant.io/readme/badge/gentics/mesh" alt="CLA assistant" />
 </a>
</p>

<br />

<p align="center">
  <a href="https://getmesh.io/">
    <img src="https://getmesh.io/assets/mesh-heroimg.png" alt="Gentics Mesh Screenshot" style="max-width: 80%" />
  </a>
</p>

# Quick Start

You can take a look at our demo application here:

* https://demo.getmesh.io/demo for the website
* https://demo.getmesh.io/mesh-ui for the CMS (login is admin/admin)

If you want to run the demo locally on your machine, the fastest way to get
started is using docker:

```bash
docker run -p 8080:8080 gentics/mesh-demo:latest
```

If you can't use docker, go to the [download
page](https://getmesh.io/download/) and get the JAR file, which you can then
execute from the command line with:

```bash
java -jar mesh-demo-v.v.v.jar
```
<!-- XXX replace latest and v.v.v with up-to-date version XXX -->

Either way, now relax for a minute while the download and the initial database
setup is performed. Then you can access the demo website locally at
http://localhost:8080/demo and the CMS at http://localhost:8080/mesh-ui .

Check out our [Getting Started Guide](https://getmesh.io/docs/getting-started/)
for an explanation of the basic concepts of Gentics Mesh.

# Table of Contents

<!-- re-generate with https://github.com/ekalinin/github-markdown-toc -->

   * [Quick Start](#quick-start)
   * [Table of Contents](#table-of-contents)
   * [Introduction](#introduction)
      * [What is a Headless CMS?](#what-is-a-headless-cms)
      * [Why pick Gentics Mesh?](#why-pick-gentics-mesh)
   * [Status](#status)
   * [Installation](#installation)
   * [Features](#features)
   * [Documentation](#documentation)
   * [Getting Help](#getting-help)
   * [Professional Services](#professional-services)
      * [Product Development](#product-development)
      * [Consulting](#consulting)
      * [Maintenance](#maintenance)
      * [Support](#support)
   * [Getting started developing with Gentics Mesh](#getting-started-developing-with-gentics-mesh)
      * [Authentication](#authentication)
      * [Using the REST API](#using-the-rest-api)
      * [Using the GraphQL API](#using-the-graphql-api)
   * [Contributing to Gentics Mesh](#contributing-to-gentics-mesh)
   * [Copyright &amp; License](#copyright--license)

# Introduction

## What is a Headless CMS?

Traditional CMSes are "coupled", which means that the CMS also takes care of
the presentation layer responsible for delivering the content to the
clients. The content and the presentation are closely interlinked. Typically,
content managers create and manage their content through tools like WYSIWYG
editors. The CMS then delivers the content according to the front-end delivery
layer built into the CMS. Typically, a traditional CMS supports your websites
but not much else.

A pure headless CMS is different, because it offers no front-end capabilities
at all, giving you full control of your customer experience via APIs. The CMS
typically provides content managers with a presentation and channel agnostic
way of managing content. It requires a front-end development team to manage the
rest with the frameworks and tools they prefer: The content can be loaded by
external applications which handle the content delivery to the client, meaning
that the content can be (re-)used by multiple applications and channels (web,
mobile app, audio guides, IOT).

<p align="center">
    <img src="https://getmesh.io/blog/gentics-mesh-1-0/headless-vs-coupled.png" alt="a coupled vs a headless CMS" style="max-width: 80%" />
</p>


## Why pick Gentics Mesh?

Gentics Mesh is the platform that holds your content, gives you great APIs to
access and modify it, while relieving you from handling permissions,
multi-language aspects, search, and much more. You can use its modern user
interface that makes content editing and administration enjoyable. At the same
time, we don’t lock you in: You choose the technology and programming language
you want to implement your application with. You decide whether you want to
host it locally or in the cloud. We don’t care if you’re a Windows, Linux or
Mac guy or girl. Also, with its Apache license, you are free to use it, modify
it and improve it.

What makes Gentics Mesh special over other headless CMSes is:

* its built-in user management not only covers the admin and editor features,
  but can also be used for handling access to your application.
* it thinks in content trees (just like websites do), which brings you many
  things for free: automatic navigation menus, automatic beautiful URLs,
  built-in link resolving, …​
* it is scalable and built for clustering, so your deployment can grow with
  your project’s success.


# Status

Gentics Mesh is actively developed by a dedicated team at APA-IT in Vienna,
Austria.

<p align="center">
    <img src="https://getmesh.io/blog/gentics-mesh-1-0/gentics-team.jpg" alt="photo of the Gentics Mesh team" style="max-width: 80%" />
</p>

The Gentics Mesh core team consists of:

<p align="center">
 <table align="center" style="width: 100%; max-width: 600px;">
  <tr>
   <td align="center">
    <a href="https://github.com/Jotschi">
     <img src="https://avatars.githubusercontent.com/Jotschi?size=150" width="150" /><br />
     Johannes Schüth<br /><i>lead developer</i>
    </a>
   </td>
   <td align="center">
    <a href="https://github.com/pschulzk">
     <img src="https://avatars.githubusercontent.com/pschulzk?size=150" width="150" /><br />
     Philip Viktor Schulz-Klingauf<br /><i>UI/UX developer</i>
    </a>
   </td>
  </tr>
  <tr>
   <td align="center">
    <a href="https://github.com/npomaroli">
     <img src="https://avatars.githubusercontent.com/npomaroli?size=150" width="150" /><br />
     Norbert Pomaroli<br /><i>software architect</i>
    </a>
   </td>
  </tr>
 </table>
</p>

<!-- XXX Roadmap XXX -->

# Installation

For your first steps, please refer to the [Quick Start](#quick-start) which
shows you how to run the demo application.

When you're ready to run your own CMS installation, check out our
[Administration Guide](https://getmesh.io/docs/administration-guide/) where you
will find everything you need to know, and much more.


# Features

* [GraphQL API](https://getmesh.io/docs/graphql/)
* [content events via websocket](https://getmesh.io/docs/events/)
* [user, role and permission management](https://getmesh.io/docs/features/#_permissions)
* [document-level permissions](https://getmesh.io/docs/features/#_permissions)
* [versioned content](https://getmesh.io/docs/features/#versioning)
* [a webroot API for easy integration with modern routing frameworks](https://getmesh.io/docs/features/#webroot)
* [a search API powered by Elasticsearch](https://getmesh.io/docs/elasticsearch/)
* [an image manipulation API](https://getmesh.io/docs/features/#imagemanipulation)
* [a tagging API](https://getmesh.io/docs/building-blocks/#_tag)
* [clustering support](https://getmesh.io/docs/clustering/)
* [monitoring support](https://getmesh.io/docs/monitoring/)
* an embedded graph database at its core
* support for [Docker](https://getmesh.io/docs/deployment/#_docker)

# Documentation

You can find our extensive documentation at https://getmesh.io/docs/

# Getting Help

You can chat with us via our [gitter channel](https://gitter.im/gentics/mesh)
or create [Stack Overflow
questions](https://stackoverflow.com/questions/tagged/gentics-mesh). Issues can
be reported via [GitHub](https://github.com/gentics/mesh/issues).

# Professional Services

The company behind Gentics Mesh, APA-IT Informations Technologie GmbH, is
offering commercial services for Gentics Mesh. Please contact us at
mesh@gentics.com for details.

## Product Development

Gentics Mesh already has an extensive set of features, but these features can
always be extended. After an extension request has been received from you, it
will be evaluated whether the features will be added to the standard product or
offered as an individual project.

## Consulting

We accompany you in your projects with our experienced Gentics Mesh
Consultants. Here we can help you setting up the project, executing together
the conception of the structures in the CMS, taking into account your
requirements, and offer orientation to best practices.

## Maintenance

Product Maintenance includes product error removal. We offer various Product
Maintenance Packages - differing between Essential, Professional and Elite
Package - depending on the number of Gentics Mesh Nodes.

## Support

To help and support you and your developers working and developing applications
and online projects using Gentics Mesh we offer a dedicated Developer Support
focusing on all the typical questions and needs arising when working with
Headless CMS solutions.


# Getting started developing with Gentics Mesh

## Authentication

First things first: you need to authenticate, otherwise you will not be able to
access your data.

* http://localhost:8080/api/v2/auth/login

You can post your credentials via JSON, use basic auth or send a JWT header -
the choice is yours. If you open that URL in a browser, you will most likely
authenticate using basic auth.

## Using the REST API

Some sample API requests:

* List users [/users](https://demo.getmesh.io/api/v2/users)
* List nodes [/demo/nodes?perPage=5](https://demo.getmesh.io/api/v2/demo/nodes?perPage=5)
* Load by path [/demo/webroot/yachts/indian-empress](https://demo.getmesh.io/api/v2/demo/webroot/yachts/indian-empress)
* Load Image [/demo/webroot/images/yacht-pelorus.jpg?w=700](https://demo.getmesh.io/api/v2/demo/webroot/images/yacht-pelorus.jpg?w=700)


## Using the GraphQL API

[A sample GraphQL query](https://demo.getmesh.io/api/v2/demo/graphql/browser/#query=query%20webroot(%24path%3A%20String)%20%7B%0A%20%20node(path%3A%20%24path)%20%7B%0A%20%20%20%20...%20on%20vehicle%20%7B%0A%20%20%20%20%20%20fields%20%7B%0A%20%20%20%20%20%20%20%20name%0A%20%20%20%20%20%20%20%20description%0A%20%20%20%20%20%20%20%20vehicleImage%20%7B%0A%20%20%20%20%20%20%20%20%20%20uuid%0A%20%20%20%20%20%20%20%20%20%20path%0A%20%20%20%20%20%20%20%20%20%20...%20on%20vehicleImage%20%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20fields%20%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20image%20%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20height%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20width%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20dominantColor%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%7D%0A%20%20%20%20%20%20%20%20%20%20%20%20%7D%0A%20%20%20%20%20%20%20%20%20%20%7D%0A%20%20%20%20%20%20%20%20%7D%0A%20%20%20%20%20%20%7D%0A%20%20%20%20%7D%0A%20%20%7D%0A%7D%0A&variables=%7B%22path%22%3A%20%22%2Fyachts%2Fpelorus%22%7D)

```
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
            fields {
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

# Contributing to Gentics Mesh

Please read our [Contributing Guidelines](https://getmesh.io/docs/contributing/) if you intend to contribute to the project. The guidelines will tell you the
legal stuff, give you instructions on how to set up your IDE in order to build Gentics Mesh, and will tell you necessary knowledge to understand the codebase.

Gentics Mesh is currently split into two repositories:

* https://github.com/gentics/mesh contains the backend code
* https://github.com/gentics/mesh-ui contains the frontend code


# Copyright & License

Copyright 2014-2020 APA-IT Informations Technologie GmbH. Licensed under the
Apache License, Version 2.0.

Gentics is a registered trade mark of APA-IT Informations Technologie GmbH.
