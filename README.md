<h1 align="center">
  <a href="https://gentics.com/mesh">
    <img src="https://www.gentics.com/mesh/assets/gentics-mesh-logo.png" width="420" alt="Gentics Mesh" />
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
 <a href="https://cla-assistant.io/gentics/mesh">
  <img src="https://cla-assistant.io/readme/badge/gentics/mesh" alt="CLA assistant" />
 </a>
</p>

<br />

# Quick Start

Check out our [Getting Started Guide](https://www.gentics.com/mesh/docs/getting-started/)
for an explanation of the basic concepts of Gentics Mesh.

# Table of Contents

<!-- re-generate with https://github.com/ekalinin/github-markdown-toc -->

   * [Quick Start](#quick-start)
   * [Table of Contents](#table-of-contents)
   * [Introduction](#introduction)
      * [Gentics CMP](#gentics-cmp)
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

## Gentics CMP

Gentics CMP (Content Management Platform) is composed of the following components: Gentics CMS, Gentics Mesh, and Gentics Portal. Gentics CMS and Mesh are available as both open-source software (OSS) and Enterprise Edition (EE). Gentics Portal Java and PHP can only be acquired with the Enterprise Edition. You can find the EE features here: [Open-Source and Enterprise Edition](https://www.gentics.com/infoportal/cmp/ossandee/). For more information about the EE, please contact [sales@gentics.com](mailto:sales@gentics.com).

Here are the links to our open-source projects:

- Gentics CMS OSS: https://github.com/gentics/cms-oss
- Gentics Mesh OSS: https://github.com/gentics/mesh

## What is a Headless CMS?

Traditional CMSes are "coupled", which means that the CMS also takes care of
the presentation layer responsible for delivering the content to the
clients. The content and the presentation are closely interlinked. Typically,
content managers create and manage their content through tools like "What you see is what you get" (WYSIWYG)
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

## Why pick Gentics Mesh?

Gentics Mesh is the platform that holds your content, gives you great APIs to
access and modify it, while relieving you from handling permissions,
multi-language aspects, search, and much more. You can use its modern user
interface that makes content editing and administration enjoyable. At the same
time, we don’t lock you in: You choose the technology and programming language
you want to implement your application with. You decide whether you want to
host it locally or in the cloud. We don’t care if you’re a Windows, Linux or
Mac guy or girl. Also, with its Apache 2.0 license, you are free to use it, modify
it and improve it.

What makes Gentics Mesh special over other headless CMSes is:

* its built-in user management not only covers the admin and editor features,
  but can also be used for handling access to your application.
* it thinks in content trees (just like websites do), which brings you many
  things for free: automatic navigation menus, automatic beautiful URLs,
  built-in link resolving, …​
* it is scalable and clustering-friendly, so your deployment can grow with
  your project’s success.

# Status

Gentics Mesh is actively developed by a dedicated team at Gentics Software GmbH in Vienna,
Austria.

The Gentics Mesh core team consists of the following members:

<p align="center">
 <table align="center" style="width: 100%; max-width: 600px;">
  <tr>
   <td align="center">
    <a href="https://github.com/deckdom">
     <!-- <img src="https://avatars.githubusercontent.com/pschulzk?size=150" width="150" /><br /> -->
     Dominik Decker<br /><i>UI/UX developer</i>
    </a>
   </td>
  </tr>
  <tr>
   <td align="center">
    <a href="https://github.com/npomaroli">
     <!-- <img src="https://avatars.githubusercontent.com/npomaroli?size=150" width="150" /><br /> -->
     Norbert Pomaroli<br /><i>Software architect</i>
    </a>
   </td>
  </tr>
    <tr>
   <td align="center">
    <a href="https://github.com/yrucrem">
     <!-- <img src="https://avatars.githubusercontent.com/pschulzk?size=150" width="150" /><br /> -->
     Patrick Klaffenböck<br /><i>Backend developer</i>
    </a>
   </td>
  </tr>
    </tr>
    <tr>
   <td align="center">
    <a href="https://github.com/plyhun">
     <!-- <img src="https://avatars.githubusercontent.com/pschulzk?size=150" width="150" /><br /> -->
     Serhii Plyhun<br /><i>Backend developer</i>
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
[Administration Guide](https://www.gentics.com/mesh/docs/administration-guide/) where you
will find everything you need to know, and much more.


# Features

* [GraphQL API](https://www.gentics.com/mesh/docs/graphql/)
* [content events via websocket](https://www.gentics.com/mesh/docs/events/)
* [user, role and permission management](https://www.gentics.com/mesh/docs/features/#_permissions)
* [document-level permissions](https://www.gentics.com/mesh/docs/features/#_permissions)
* [versioned content](https://www.gentics.com/mesh/docs/features/#versioning)
* [a webroot API for easy integration with modern routing frameworks](https://www.gentics.com/mesh/docs/features/#webroot)
* [a search API powered by Elasticsearch](https://www.gentics.com/mesh/docs/elasticsearch/)
* [an image manipulation API](https://www.gentics.com/mesh/docs/features/#imagemanipulation)
* [a tagging API](https://www.gentics.com/mesh/docs/building-blocks/#_tag)
* [clustering support](https://www.gentics.com/mesh/docs/clustering/)
* [monitoring support](https://www.gentics.com/mesh/docs/monitoring/)
* an embedded graph database at its core
* support for [Docker](https://www.gentics.com/mesh/docs/deployment/#_docker)

# Documentation

You can find our extensive documentation at https://www.gentics.com/mesh/docs/

# Getting Help

You can chat with us via our [gitter channel](https://gitter.im/gentics/mesh)
or create [Stack Overflow
questions](https://stackoverflow.com/questions/tagged/gentics-mesh). Issues can
be reported via [GitHub](https://github.com/gentics/mesh/issues).

# Professional Services

The company behind Gentics Mesh, Gentics Software GmbH is
offering commercial services for Gentics Mesh. Please contact us at
sales@gentics.com for details.

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

The full REST API documentation is available at the [Gentics Infoportal](https://www.gentics.com/mesh/docs/api/#).

## Using the GraphQL API

The data fetching [GraphQL API](https://www.gentics.com/mesh/docs/api/#project__graphql__post) is a part of the public REST API. 
The `IntrospectionQuery` is supported as well, so one can use a REST API tool with GraphQL support to have a code-assisted experience while writing own GraphQL queries. 

An example of a GraphQL query looks as follows:

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

Please read our [Contributing Guidelines](https://www.gentics.com/mesh/docs/contributing/) if you intend to contribute to the project. The guidelines will tell you the
legal information, give you instructions on how to set up your IDE in order to build Gentics Mesh, and will tell you necessary knowledge to understand the codebase.

* https://github.com/gentics/mesh contains the Gentics Mesh code

# Copyright & License

Copyright (c) 2014-2025 Gentics Software GmbH. Licensed under the
Apache License, Version 2.0.

Gentics is a registered trade mark of Gentics Software GmbH.
