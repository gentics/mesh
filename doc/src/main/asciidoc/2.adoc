# Intro

 * [Graph Model](http://gist.neo4j.org/?693a270722b01d647849)
 
## Verticles

CaiLun consists of various [Vert.x](http://vertx.io/) verticles. Each verticle provides REST endpoints that enable CRUD operations on core entities.

ACL Verticles:

 * UserVerticle
 * GroupVerticle
 * RoleVerticle

Content Verticles:

 * TagVerticle
 * ContentVerticle

Project Structure Verticles:
 
 * ProjectVerticle
 * ObjectSchemaVerticle

Other:

 * AdminVerticle

## Core Entites

### ACL Entities

Users can be assigned to groups. Roles are assigned to groups.

TODO: Explain permission system

 * User
 * Group
 * Role

### Content Entities 

 There are three basic types of contents. 

 * BinaryFile - BinaryFiles like images or videos 
 * Content    - A content can be a blogpost or a page
 * Tag        - Tags can be used to create a folders structure

### Structure Entities

 * ObjectSchema  - Schemas are used to specify custom types for Tags and Contents 
 * Project - A project is the root node for a tag and content structure

## Tagging

tbd

## Contents

tbd
