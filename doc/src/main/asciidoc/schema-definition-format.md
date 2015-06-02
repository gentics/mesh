# Schema Definition JSON

Basic schema definition format
```
{
  "name": "MySchema",
  ["binary": boolean,]
  ["container": boolean,]
  ["displayField": boolean,]
  "fields": []
}
```

## Example Schema

```json
{
  "name": "product",
  "displayField": "name",
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
  "allow": ["vcard", "mediaObject"]
}
```
