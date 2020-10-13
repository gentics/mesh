# Export Plugin

A Mesh-Plugin to export any Mesh content dynamically into a CSV File.

## Building:

build the Plugin with maven
```bash
mvn clean package
```
after building, take the "mesh.plugin.export-PLUGIN-VERSION.jar" from the "/target" folder.

> **Note:** if there comes a error while building with maven, try building it without running the Tests: `-Dmaven.test.skip=true`

## Installation

Simply install this plugin like any other Mesh-Plugin by putting it into the `/plugins` Folder.
You can then manually deploy it when the server is still running or restart the server to load it on startup.

## Configuration

The only configuration-options are the webroot-paths to the config-nodes which are used for `CONFIG_VALUE` mappings
and the cache timing of these config-nodes.

If the webroot-path does not exist, it is simply ignored.
The format of the configuration-nodes need to look like this:

Configuration Schema:
```json
{
  "name": "example_configuration",
  "segmentField": "slug",
  "fields": [
    {
      "name": "slug",
      "type": "string"
    },
    {
      "name": "exampleOption",
      "type": "list",
      "listType": "micronode",
      "allow": [
        "example_option"
      ]    
    }
  ]
}
```

Option Micro-Schema:
```json
{
  "name": "example_option",
  "fields": [
    {
      "name": "id",
      "type": "string"
    },
    {
      "name": "value",
      "type": "string"
    }
  ]
}
```

## Request

To export the relevant data to a CSV File, we use a Subset of a regular GraphQL request.
The endpoint registers to each Project and can be called the following way:

`POST /api/v2/<project>/plugins/export/csv`

The body has following format:

```json5
{
  "query": "<GRAPHQL-QUERY>",
  "variables": {
    "query": "<ELASTICSEARCH-QUERY>"
  },
  "mapping": [
    // <MAPPING-ENTRIES>
  ]
}
```

In `query`, you define the GraphQL Query to use when loading the nodes.
The resulting data can be nested to any depth, but it should only be loaded what is necessary.
It also **needs** a proper GraphQL `query` definition like this:
```graphql
query ($query: String, $page: Long, $perPage: Long) {
  nodes(query: $query, page: $page, perPage: $perPage) {
    elements {
      uuid
    }
  }
}
```

In `variables.query`, you define the Elasticsearch-Query which is used to filter the nodes you want to export.

Finally, in `mapping`, you define the mappings on how the columns are getting rendered from the returned GraphQL result.
See next section for further info.

## Mapping

A mapping represents a Single Column, or a Multi Column (Depending on the Type) in the CSV File.
The only important difference between these, is how expended multi-values are being output.

Following properties are available for mappings:

| Name                   | Type                  | Mapping-Types  | Required                   | Description |
| ---------------------- | --------------------- | -------------- | -------------------------- | ----------- |
| `path`                 | String                | All            | Required                   | The Path to the property that should be rendered. Ignores any arrays and simply appends all values together. |
| `type`                 | [Type](#types)        | All            | Required                   | The Type of the property that should be rendered. See next section for further info. |
| `label`                | String                | All            | Required for single-column | The label for the Column. |
| `filter`               | [Filter](#filtering)  | All            | Optional                   | A filter object to filter the entire (multi) column. See #Filtering for further info. |
| `dateFormat`           | String                | `DATE`         | Optional                   | Custom Date (`java.time.format.DateTimeFormatter`) Format. |
| `configKey`            | String                | `CONFIG_VALUE` | Reqiured                   | The key from the combined config to load the mapping from. |
| `configValueField`     | String                | `CONFIG_VALUE` | Optional                   | Which field of the mapping to use for the value. Defaults to `"value"`|
| `listType`             | [Type](#types)        | `LIST`         | Required                   | The type of element that is in the list.
| `listExpand`           | Boolean               | `LIST`         | Optional                   | If it should create a new Row for each value instead of joining all values together. |
| `listExpandLimit`      | Number                | `LIST`         | Optional                   | The limit of how many elements should be expanded into their own Row. |
| `subMapping`           | [Mapping](#mapping)[] | `SUB_NODE`, `FOREIGN_REFERENCE` | Required  | An addtional Array of Mappings for the sub-elements, relative to each element. |
| `subFilter`            | [Filter](#filtering)  | `SUB_NODE`, `FOREIGN_REFERENCE` | Optional  | A Filter to filter out individual sub-elements. |
| `referenceGraphQuery`  | String                | `FOREIGN_REFERENCE`             | Required  | The GraphQL query for loading the foreign Node References. |
| `referenceSearchQuery` | String                | `FOREIGN_REFERENCE`             | Required  | The ElasticSearch query for filtering the foreign Node References. It replaces `%uuid%` with the current Node UUID. |
| `referenceMaxCount`    | Number                | `FOREIGN_REFERENCE`             | Optional  | Maximal amount of Nodes that should be loaded/output. |

> **Note**: When the Type is not `LIST`, all values but one gathered from the `path` are getting dropped.

## Types

| Name                 | Type     | Description                                                                          |
| -------------------- | -------- | ------------------------------------------------------------------------------------ |
| `STRING`             | Single   | Renders the string value without additional formatting.                              |
| `NUMBER`             | Single   | Renders the number in a Excel friendly format.                                       |
| `BOOLEAN`            | Single   | Renders the boolean as `0` and `1` representations.                                  |
| `DATE`               | Single   | Renders the date with the provided format. Defaults to a Excel friendly Date format. |
| `CONFIG_VALUE`       | Single   | Attempts to map the value to the provided config and outputs the mapped value.       |
| `LIST`               | Meta     | Meta Type to render multiple values. Must be used in combination of another Type.    |
| `SUB_NODE`           | Multi    | Used for micronodes and directly referenced nodes to group the values together.      |
| `FOREIGN_REFERENCE ` | Multi    | Loads all Foreign Node-References of the currently processed Node and maps them to the sub-mapping. |

## Multi-Column

With Multi-Column mappings, you need to specify a `subMapping`, which is relative to the current object.
The label in this Mapping is getting ignored and is not required.

You can also filter each element of the result via the `subFilter` property.
The paths for filtering are then relative to the filtered object instead of the root-node.

When the elements are getting expended to multiple rows with the combination of the `LIST` Type,
then each element stays inside of the boundry of the group-column and creates a proper matrix with
all variations in the resulting CSV file. 

## Filtering

 Filtering is done on a (Single or Multi-)Column basis and should only be used where regular ElasticSearch filtering does not work.
 It allows you to filter out elements which you might not want to output in the CSV File.

 There's currently only two filters which can be used together with grouping- and meta-filters.
 You may ever only specify one type of filter in a filter object (similar to an elasticsearch query-object).
 Following Types are available:

 | Name     | Type    | Value    | Description                                                        |
 | -------- | ------- | -------- | ------------------------------------------------------------------ |
 | `and`    | Group   | Filter[] | All provided filters must match. Must specify at least one filter. |
 | `or`     | Group   | Filter[] | One or more of provided filter must match.                         |
 | `not`    | Meta    | Filter   | Inverts the result of the provided Filter.                         |
 | `equals` | [Equals](#equals) || Checks if the element-value and the filter-value are the same.     |
 | `range`  | [Range](#range)   || Checks if the element-value is in the range of the filter-range.   |

---

Example:

```json
{
  "and": [
    {
      "not": {
        "equals": {
          "path": "fields.myField",
          "value": null
        }
      },
      {
        "range": {
          "path": "created",
          "gte": "2020-01-01T00:00:00Z"
        }
      },
      {
        "or": [
          {
            "equals": {
              "path": "fields.myType",
              "value": 1
            }
          },
          {
            "equals": {
              "path": "fields.myType",
              "value": 2
            }
          },
          {
            "equals": {
              "path": "fields.myType",
              "value": 3
            }
          }
        ]
      }
    }
  ]
}
```

 ### Equals

Checks if the element-value and the filter-value are the same.

| Property  | Type     | Required | Description                                            |
| --------- | -------- | -------- | ------------------------------------------------------ |
| `path`    | String   | Required | Path to the element-value.                             |
| `value`   | Anything | Optional | The filter-value to check against. Defaults to `null`. |

The Equality check is based on the `java.util.Objects.equals` implementation: `Objects.equals(filterValue, elementValue)`.

> **Note**: When no value from `path` could be found, it's defaulting to `null`.

 ### Range

 Checks if the element-value is in the range of the filter-range.

 | Property | Type         | Required | Description                                                      |
 | -------- | ------------ | -------- | ---------------------------------------------------------------- |
 | `path`   | String       | Required | Path to the element-value. Must point to a Number or Date field. |
 | `gt`     | Number, Date | Optional | Value to check if it's *greater than* to it.                     |
 | `gte`    | Number, Date | Optional | Value to check if it's *greater than or equal* to it.            |
 | `lt`     | Number, Date | Optional | Value to cehck if it's *lower than* to it.                       |
 | `lte`    | Number, Date | Optional | Value to cehck if it's *lower than or equal* to it.              |

While the Fields `gt`, `gte`, `lt`, and `lte` are optional, one of these must be defined.
You may also not define `gt` and `gte`, or `lt` and `lte` together:

```json5
// Invalid
{
  "range": {
    "path": "fields.value",
    "gt" 1,
    "gte": 2
  }
}
// Valid
{
  "range": {
    "path": "fields.value",
    "gt": 1,
    "lt" 10
  }
}
```

> **Note**: When no value from `path` could be found, the Filter will fail.
