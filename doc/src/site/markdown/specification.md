# Functional specification

## Tagging

There is only one root tag per project. The root tag is linked to various other tags which can be of different types. Tags can have i18n properties.

A tag must have a specify schema. Schemas can be created using the schemas verticle. The schema defines the type and name of each property.

A tag must only have a single parent.

The root Tag of a project can not be deleted.

## Groups

Groups are used to organize users. Roles can be assigned to groups. A user in a group with roles inherits those roles and the linked permissions to those roles.

Groups are not nested.

## Roles

Roles are used to assign permissions to objects. Roles are assigned to groups. The create permission for a group enabled the creation of roles.

Users can only assign permissions to roles to which they have access.

A special permission or the update permission of a role (yet to be determined) enables users to create / update permissions on objects.