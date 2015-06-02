# Schema Definition Format

### Review:

Maybe we could get rid of tags altogether. A node could have fields called `colorTags` and `vehicleTags`. The user could then assign nodes to those fields. The color Tags would be regular nodes with the schema `colorTag`. The color tags would be located within the project node hierarchy. (e.g.: /colorTags/red). The root folder colorTags and the tag red must be invisbile and not be accessiable though /webroot.

### Contents:

* [Example Schema](#example-schema)
* [Field Definition Object](#field-definition-object)
* [Configuration Properties](#configuration-properties)
* [Default Values](#default-values)
* [Types](#types)
* [Microschema Definition Format](#microschema-definition-format)

## Example Schema

```json
{
  "name": "product",
  "displayField": "name",
  "container": true,
  "binary": false,
  "fields": [
    {
      "name": "name",
      "label": "Product Name",
      "type": "string",
      "required": true,
    },
    {
      "name": "description",
      "label": "Description",
      "type": "html",
      "required": true,
    },
    {
      "name": "image",
      "label": "Product Image",
      "type": "node",
      "allow": ["image"]
    },
    {
      "name": "sku",
      "label": "SKU",
      "type": "string",
      "required": true
    },
    {
      "name": "price",
      "label": "Price",
      "type": "number",
      "step": 0.01
     },
     {
      "name": "relatedProducts",
      "label": "Related Products",
      "type": "list",
      "listType": "node",
      "allow": ["product"]
      }
  ]
}
```

## Field Definition Object

A field is defined by an object which must have the following properties:

* **`name`** A unique name to identify the field
* **`type`** The type of data to be stored in this field (see [Types](#types))

The following optional properties may be applied to any type of field:

* **`required`** If `true`, this field may not be left empty.
* **`label`** A human-readable label for the field to be used as a form label in the admin UI. If not defined, the "name" field would be used.
* **`defaultValue`** A default value for the field. See [Default values](#default-values).

```json
{
  "name": "title",
  "type": "string",
  "label": "Blog Post Title",
  "required": true,
  "defaultValue": "New Blog Post"
}
```

In addition to the above, certain types expose additional properties with which to configure the field. Such additional
properties are defined in the [Types](#types) section.

## Configuration properties

The following configuration properties may be specified:

* **`binary`** Set to `true` to mark the schema as a binary schema. This will cause a file upload input to appear 
when creating/editing a node using this schema, and also add additional binary-specific properties to the response object
of such nodes (see [json-node-response-format.md](json-node-response-format.md) for an example).
* **`container`** Set to `true` to indicate that this schema can contain child nodes. This will cause the response 
object for such nodes to have a `children` property.
* **`displayField`** Used to specify which field (as defined in the "fields" list) should be considered the title for
the purpose of displaying a list of nodes. The value must be a string which corresponds to the name of one of the
schema's fields, and additionally that field must not be of type "list", "node", or "microschema".

## Default values

A default value for a field can be specified by providing the "defaultValue" property (*NB: I did not choose "default", 
since this is a reserved word in JavaScript and therefore would lead to issues in accessing the property using dot notation*). 

The expected format of the defaultValue property would depend on the type of the field - e.g. a "string" field would expect 
a string as the defaultValue, and a "select" field would expect the defaultValue to match one of the items in the "options" 
array.

```json
{
  "name": "promoted",
  "label": "Promoted Post",
  "type": "boolean",
  "defaultValue": false
}
```

```json
{
  "name": "externalLinks",
  "label": "External Links",
  "type": "list",
  "listType": "string",
  "defaultValue": ["http://www.example.com", "http://www.test.com"]
}
```

## Types

"type" can be one of the following:
* string
* html
* number
* date
* boolean
* select
* node
* list
* microschema

### String type

A string type does not have any special config settings.

### HTML type

TODO: decide on any special config properties for HTML type, e.g. allowed tags.

### Number type

A number field has three optional config properties: "min" is the lowest permitted value, "max" is the greatest permitted 
value, and "step" is the size of the permitted increment in value. For example:

```json
{
  "name": "probability",
  "label": "Probability",
  "type": "number",
  "min": 0,
  "max": 1,
  "step": 0.01
}
```

### Date type

TODO: decide on any special config properties for date type.
TODO: Maybe a default timeframe would be useful.

### Boolean type 

A string type does not have any special config settings.

### Select type

A select type is used to display an enumeration of options, typically for use in a `<select>` input (or radio buttons), 
and would be defined by an "options" property:

```json
{
  "name": "color",
  "label": "Color",
  "type": "select",
  "options": ["blue", "red", "green", "heliotrope"]
}
```

TODO: Think about usecases for the select type. Should the user be able to select a node from a predefinded subset of nodes? Should the user be able to select a tag? How to handle multiselect?

### Tag type (idea)

A tag type is a type that references a tag. The tag type can also be used within a list. By that it is possible to add multiple tags to different fields of a node.

Problem: /nodes/:uuid/tags would no longer work when we decide to remove the tags field in favour of this type.

Question: Is it possible to change the rest api that way that tags can be assigned to specific tag list fields for a node. 

* /nodes/:uuid/fields.taglistA/:tagUuid
* /nodes/:uuid/fields.taglistB/:tagUuid


### Node type

A node type must have an "allow" property which acts as a white list for schemas which may be used. If "allow" is an 
empty array, any type of node may be used.

```json
{
  "name": "resources",
  "label": "Resources",
  "type": "node",
  "allow": ["image", "file"]
}
```
or
```json
{
  "name": "relatedContent",
  "label": "Related Content",
  "type": "node",
  "allow": ["blogPost", "product"]
}
```

### List type

A list **must** be typed via the "listType" property, and can define min and max values for the list length.

```json
{
  "name": "urls",
  "label": "External Links",
  "type": "list",
  "listType": "string",
  "min": 1,
  "max": 10
}
```

a list of related content nodes would be defined thus:

```json
{
  "name": "relatedContent",
  "label": "Related Content",
  "type": "list",
  "listType": "node",
  "allow": ["blogPost", "product"],
  "min": 1,
  "max": 5
}
```

### Microschema type

A microschema field must have a property "allow", which defines the microschemas which may be used in this field. If "allow" 
is an empty array, any type of microschema may be used.

```json
{
  "name": "about",
  "label": "About Me",
  "type": "list",
  "listType": "microschema",
  "allow": ["vcard", "gallery"]
}
```

## Microschema Definition Format

A microschema is defined in much the same way as a schema. All the rules which apply to schema definitions also apply
to microschema definitions, with the following exceptions:

1. A microschema cannot contain a field of type "microschema".
2. A microschema should not use the configuration properties "binary", "container", or "displayField".

```json
{
  "name": "vcard",
  "fields": [
    {
      "name": "firstName",
      "label": "First Name",
      "type": "string"
    },
    {
      "name": "lastName",
      "label": "Last Name",
      "type": "string"
    },
    {
      "name": "address",
      "label": "Address",
      "type": "string"
    },
    {
      "name": "postcode",
      "label": "Post Code",
      "type": "string"
    }
  ]
}
```

Just as with schemas, a microschema could contain list and node types:

```json
{
  "name": "gallery",
  "fields": [
    {
      "name": "title",
      "label": "Gallery Title",
      "type": "string"
    },
    {
      "name": "image",
      "type": "list",
      "listType": "node",
      "allow": ["image"],
      "min": 1,
      "max": 20
    }
  ]
}
```
