# Export Plugin

A Mesh-Plugin to export any Mesh content dynamically into a CSV File.

- [Export Plugin](#export-plugin)
  - [Building](#building)
  - [Installation](#installation)
  - [Configuration](#configuration)
    - [WebrootPathConfiguration](#webrootpathconfiguration)
  - [Configuraion Values](#configuraion-values)
    - [Configuration Nodes](#configuration-nodes)
    - [Helper-Plugins](#helper-plugins)
      - [Setup](#setup)
      - [Implementation](#implementation)
  - [Request](#request)
    - [Query](#query)
    - [Variables](#variables)
  - [Mapping](#mapping)
  - [Types](#types)
  - [Multi-Column](#multi-column)
  - [Filtering](#filtering)
    - [Equals](#equals)
    - [Range](#range)
    - [In](#in)
  - [Misc Configurations](#misc-configurations)
    - [Boolean Mapping](#boolean-mapping)

## Building

Build the Plugin with maven:

```bash
mvn clean package
```

After building, take the build Plugin `mesh-export-plugin-{VERSION}.jar` from the `target` folder.

> **Note:** If there comes a error while building with maven, try building it without running the Tests (as there currently are none):
> 
> `-Dmaven.test.skip=true` / `-DskipTests`
> ```bash
> $ mvn -Dmaven.test.skip=true clean package
> # Or
> $ mvn -DskipTests clean package
> ```

## Installation

Simply install this plugin like any other Mesh-Plugin by putting it into the `/plugins` Folder.
You can then manually deploy it when the server is still running or restart the server to load it on startup.

## Configuration

In the configuration you may configure webroot-paths to fetch configuration-nodes; cache-times for config-values provided by `MappingProviders`; and the role-name to access the export endpoint.

| Setting                  | Type                         | Default Value   | Description                                                        |
| ------------------------ | ---------------------------- | --------------- | ------------------------------------------------------------------ |
| `webrootPaths`           | `WebrootPathConfiguration[]` | null            | Webroot Paths to the configuration nodes.                          |
| `cacheTime`              | `Integer`                    | `600000`        | How long the configuration nodes are being cached in milliseconds. |
| `roleCheck`              | `Boolean`                    | `false`         | If it should check and create the role (if needed) on startup.     |
| `roleName`               | `String`                     | `plugin_export` | The Role name to control who has access to the export.             |

### WebrootPathConfiguration

A single Webroot path can be configured to be used in multiple projects.
If you leave the projects empty, it'll use it on all projects.

| Setting       | Type       | Required | Description |
| ------------- | ---------- | -------- | ----------- |
| `path`        | `String`   | Yes      | The webroot-path to the configuration-node. Slashes in the beginning and end are stripped. |
| `projects`    | `String[]` | No       | A list of project-names for which this path is valid for. If none is specified, it's available for all projects. |

## Configuraion Values

Configuration Values are used for `CONFIG_VALUE` mappings to map the value to a proper output value.

This is usually used for Options, i.e:
* "0" -> "Unknown"
* "1" -> "Male"
* "2" -> "Female"
* "3" -> "Different"

### Configuration Nodes

Currently the only out of the box supported possibility is to create "Configuration Nodes" which are loaded with the ["WebrootPathConfiguration"](#webrootpathconfiguration) options.

The format of the configuration-nodes should to look like this:

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

> ***NOTE***: You do not have to name your segmentField `slug`, it's just a convention.
> You may also add more unrelated fields, as all fields which are not a micro-node list
> and do not have a `id` and `value` string field will be **ignored**.

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

Configuration Node:
```json
{
  "language": "en",
  "schema": {
    "name": "example_configuration"
  },
  "parentNode": {
    "uuid": "00112233445566778899aabbccddeeff"
  },
  "fields": {
    "slug": "config",
    "exampleOption": [
      {
        "microschema": { "name": { "example_option" } },
        "fields": { "id": "1", "value": "Example Option Value 001" }
      },
      {
        "microschema": { "name": { "example_option" } },
        "fields": { "id": "2", "value": "Another Example Option Value 002" }
      },
      {
        "microschema": { "name": { "example_option" } },
        "fields": { "id": "3", "value": "Some other Option Value 003" }
      }
    ]
  }
}
```

Plugin Configuration:
```yml
webrootPaths:
- path: "/config"
```

Example Mapping:
```json
{
  "path": "fields.name",
  "label": "Name",
  "type": "STRING"
},
{
  "path": "fields.someOptionField",
  "label": "Value Label",
  "type": "CONFIG_VALUE",
  "configKey": "exampleOption"
}
```

Example Node:
```json5
{
  // ...
  "fields": {
    "name": "Example Node 01",
    "someOptionField": "1"
  }
  // ...
}
```

Output:
```csv
"Name";"Value Label"
"Example Node 01";"Example Option Value 001"
```

---

### Helper-Plugins

When regular Nodes via Webroot aren't your cup of tea, then you can create a Helper-Plugin which gets you your Config-Values with custom logic.

#### Setup

First, you need to add a dependency to the export-plugin extension for the interfaces to implement:

```xml
<dependency>
    <groupId>com.gentics.mesh.plugin.commercial</groupId>
    <artifactId>mesh-export-plugin-extension-api</artifactId>
    <version>YOUR_VERSION_HERE</version>
    <scope>provided</scope>
</dependency>
```

> ***IMPORTANT***: The scope has to be set to `provided`, otherwise your Plugin will not
> be detected by the Export-Plugin and won't get used!

Additionally, you should add the Export-Plugin as dependency to your Plugin, or it might load in the
wrong order which will break your Plugin.
To do so, simply add a new `Plugin-Dependencies` entry to your Plugin-Manifest like so:

```
Plugin-Dependencies: export
```

See https://pf4j.org/doc/plugins.html#notes-about-plugin-dependencies for more information.

#### Implementation

Implementation is quite simple: Implement the [`ExportConfigValuePlugin`](extension-api/src/main/java/com/gentics/mesh/plugin/export/extension/ExportConfigValuePlugin.java) interface into your Mesh-Plugin.
Then create a [`ConfigValueProvider`](extension-api/src/main/java/com/gentics/mesh/plugin/export/extension/ConfigValueProvider.java) and pass it out in your Plugin.

That's it! Now you get a call whenever config-values should be loaded again. Caching is already handled beforehand, so you don't have to worry about it.

For examples, you can just take a look at the [`ExportPlugin`](plugin/src/main/java/com/gentics/mesh/plugin/export/ExportPlugin.java) and the [`WebrootConfigValueProvider`](plugin/src/main/java/com/gentics/mesh/plugin/export/WebrootConfigValueProvider.java) implementation.

---

## Request

To export the relevant data to a CSV File, we use a Subset of a regular GraphQL request.
The endpoint registers to each Project and can be called the following way:

`POST /api/v2/<project>/plugins/export/csv`

The body has following format:

| Property          | Type                        | Required | Description                                                                           |
| ----------------- | --------------------------- | -------- | ------------------------------------------------------------------------------------- |
| `query`           | String                      | Required | The GraphQL Query to execute to get elements to export.                               |
| `variables`       | [`Variables`](#variables)   | Required | Variables for the GraphQL Query.                                                      |
| `mapping`         | [`Mapping`](#mapping)[]     | Required | The mapping definitions to map the data to CSV colums.                                |
| `maximalNodes`    | Integer                     | Optional | Limits the amount of Nodes (*NOT* Rows!) to export.                                   |
| `elementsPath`    | String                      | Optional | Path to where the root of the elements to export start. Defaults to `nodes.elements`. |
| `hasNextPagePath` | String                      | Optional | Path to the "hasNextPage" flag to determine if more Pages exist. Defauls to `nodes.hasNextPage`. |
| `elementFilter`   | [`Filter`](#filtering)      | Optional | Filter which filters out elements.                                                    |
| `booleanMappiung` | [`BooleanMapping`](#boolean-mapping) | Optional | Mapping for boolean values.                                                  |
| `dateFormat`      | String                      | Optional | Date-Format to use for dates.                                                         |
| `dateLocale`      | String                      | Optional | The Locale in which the date should be formatted.                                     |
| `dateZone`        | String                      | Optional | The zone-name in which the date should be formatted.                                  |
| `numberLocale`    | String                      | Optional | The Locale in which the number should be formatted.                                   |
| `numberStyle`     | `NUMBER`, `INTEGER`, `CURRENCY`, or `PERCENT` | Optional | The number style. Defaults to `NUMBER`.                             |
| `numberCurrency`  | String                      | Optional | Currency to display when the `numberStyle` is `CURRENCY`.                             |
| `numberFractionMinimum` | Number                | Optional | Minimum amount of fraction-digits to render. Defaults to `0`.                         |
| `numberFractionMaximum` | Number                | Optional | Maximum amount of fraction-digits to render. Defaults to `10`.                        |

### Query

In `query`, you define the GraphQL Query to use when loading the nodes.
The resulting data can be nested to any depth, but it should only be loaded what is necessary.
On default, a GraphQL Query like this is expected:

```graphql
query ($query: String, $page: Long, $perPage: Long) {
  nodes(query: $query, page: $page, perPage: $perPage) {
    hasNextPage
    elements {
      uuid
    }
  }
}
```

### Variables

| Property    | Type      | Required | Description                                                       |
| ----------- | --------- | -------- | ----------------------------------------------------------------- |
| `query`     | String    | Required | The Elasticsearch-Query to filter the nodes you want to export.   |
| `perPage`   | Integer   | Optional | Useful if you need to keep the memory footprint low.              |
| `page`      | Integer   | Optional | Can be used to skip first *n* pages. This is discouraged however. |

## Mapping

A mapping represents a Single Column, or a Multi Column (Depending on the Type) in the CSV File.
The only important difference between these, is how expended multi-values are being output.

Following properties are available for mappings:

| Name                    | Type                  | Mapping-Types  | Required                   | Description |
| ----------------------- | --------------------- | -------------- | -------------------------- | ----------- |
| `path`                  | String                | All            | Required                   | The Path to the property that should be rendered. Ignores any arrays and simply appends all values together. |
| `type`                  | [Type](#types)        | All            | Required                   | The Type of the property that should be rendered. See next section for further info. |
| `label`                 | String                | All            | Required for single-column | The label for the Column. |
| `filter`                | [Filter](#filtering)  | All            | Optional                   | A filter object to filter the entire (multi) column. See #Filtering for further info. |
| `subMapping`            | [Mapping](#mapping)[] | `SUB_NODE`, `FOREIGN_REFERENCE` | Required  | An addtional Array of Mappings for the sub-elements, relative to each element. |
| `subFilter`             | [Filter](#filtering)  | `SUB_NODE`, `FOREIGN_REFERENCE` | Optional  | A Filter to filter out individual sub-elements. |
| `defaultValue`          | Any                   | All            | Optional                   | The value it should use when the value could not be found or when it's null. |
| `booleanMappiung`       | [`BooleanMapping`](#boolean-mapping) | `BOOLEAN` | Optional         | Mapping for boolean values.                        |
| `configKey`             | String                | `CONFIG_VALUE` | Reqiured                   | The key from the combined config to load the mapping from. |
| `dateFormat`            | String                | `DATE`         | Optional                   | Custom Date (`java.time.format.DateTimeFormatter`) Format. |
| `dateLocale`            | String                | `DATE`         | Optional | The Locale in which the date should be formatted.                    |
| `dateZone`              | String                | `DATE`         | Optional | The zone-name in which the date should be formatted.                 |
| `listType`              | [Type](#types)        | `LIST`         | Required                   | The type of element that is in the list.            |
| `listExpand`            | Boolean               | `LIST`         | Optional                   | If it should create a new Row for each value instead of joining all values together. |
| `listExpandLimit`       | Integer               | `LIST`         | Optional                   | The limit of how many elements should be expanded into their own Row. |
| `numberLocale`          | String                | `NUMBER`       | Optional | The Locale in which the number should be formatted.                  |
| `numberStyle`           | `NUMBER`, `INTEGER`, `CURRENCY`, or `PERCENT` | `NUMBER` | Optional | The number style. Defaults to `NUMBER`.            |
| `numberCurrency`        | String                | `NUMBER`       | Optional | Currency to display when the `numberStyle` is `CURRENCY`.            |
| `numberFractionMinimum` | Number                | `NUMBER`       | Optional | Minimum amount of fraction-digits to render. Defaults to `0`.        |
| `numberFractionMaximum` | Number                | `NUMBER`       | Optional | Maximum amount of fraction-digits to render. Defaults to `10`.       |
| `referenceGraphQuery`   | String                | `FOREIGN_REFERENCE`             | Required  | The GraphQL query for loading the foreign Node References. |
| `referenceSearchQuery`  | String                | `FOREIGN_REFERENCE`             | Required  | The ElasticSearch query for filtering the foreign Node References. It replaces `%uuid%` with the current Node UUID, and `%value%` with the Value. If there's multiple values, the query is executed for each one. |
| `referenceMaximalNodes` | Integer               | `FOREIGN_REFERENCE`             | Optional  | Maximal amount of Nodes that should be loaded/output. |
| `templateDefinition`    | String                | `TEMPLATE`     | Required                   | The definition of the template. Requires to contain the replace token "%value%". |
| `regexpDefintion`       | String                | `REGEXP`       | Required                   | The regexp definition to use to capture the data of the value. |
| `regexpFlags`           | Integer               | `REGEXP`       | Optional                   | Flags to pass along with the definition. Defaults to `0`.
| `regexpTemplate`        | String                | `REGEXP`       | Required                   | The template which renders the output value. Groups from the Match may be replaced with `%<GROUP-NUM>%` pattern (i.E. `%0%`, `%1%`, etc.). |

> **Note**: When the Type is not `LIST`, all values but one gathered from the `path` are getting dropped.

## Types

| Name                 | Type     | Description                                                                          |
| -------------------- | -------- | ------------------------------------------------------------------------------------ |
| `STRING`             | Single   | Renders the string value without additional formatting.                              |
| `NUMBER`             | Single   | Renders the number in a Excel friendly format.                                       |
| `BOOLEAN`            | Single   | Renders the boolean as `0` and `1` representations.                                  |
| `DATE`               | Single   | Renders the date with the provided format. Defaults to a Excel friendly Date format. |
| `TEMPLATE`           | Single   | Renders the value with the template-definition by replacing the token `%value%` with the value. Useful for binary-fields. |
| `REGEXP`             | Single   | Renders the template by replacing the `%<GROUP-NUM>%` tokens with the matches of the definition. |
| `CONFIG_VALUE`       | Single   | Attempts to map the value to the loaded config-values and outputs the mapped value.       |
| `LIST`               | Meta     | Meta Type to render multiple values. Must be used in combination of another Type.    |
| `SUB_NODE`           | Multi    | Used for micronodes and directly referenced nodes to group the values together.      |
| `FOREIGN_REFERENCE ` | Multi    | Loads all Foreign Node-References of the currently processed Node and maps them to the sub-mapping. |

## Multi-Column

With Multi-Column mappings, you need to specify a `subMapping`, which is relative to the current object.
The label in this Mapping is getting ignored and is not required.

When the elements are getting expended to multiple rows with the combination of the `LIST` Type,
then each element stays inside of the boundry of the group-column and creates a proper matrix with
all variations in the resulting CSV file.

You can also filter each element of the result via the `subFilter` property.
The paths for filtering are then relative to the filtered object instead of the root-node.
To access the root-node again, you may use `$` as first entry.
Example:

*Node:*
```json
{
  "uuid": "00112233445566778899aabbccddeeff",
  "fields": {
    "someOtherField": true,
    "myMicroNode": [{
      "fields": {
        "nestedField": "value123"
      }
    }, {
      "fields": {
        "missingNestedField": "hello world"
      }
    }]
  }
}
```

*Mapping:*
```json
{
  "type": "SUB_NODE",
  "path": "fields.myMicroNode",
  "label": "Sample",
  "subFilter": {
    "and": [
      {
        "equals": {
          "path": "$.fields.someOtherField",
          "value": true
        },
        {
          "exists": "fields.nestedField"
        }
      }
    ]
  },
  "subMapping": [
    {
      "type": "STRING",
      "path": "fields.nestedField",
      "label": "My cool value"
    }
  ]
}
```

In this example, only the first element would pass through.

## Filtering

Filtering is done on a (Single or Multi-)Column basis and should only be used where regular ElasticSearch filtering does not work.
It allows you to filter out elements which you might not want to output in the CSV File.

There's currently only two filters which can be used together with grouping- and meta-filters.
You may ever only specify one type of filter in a filter object (similar to an elasticsearch query-object).

Paths are specified by each segment separated by `.`-characters, just like in elasticsearch or in the mapping-path.
Additionally, when using filtering in the sub-filter context, you can start the path with `$` to get back
to the root object.

Following Types are available:

| Name     | Type    | Value    | Description                                                           |
| -------- | ------- | -------- | --------------------------------------------------------------------- |
| `and`    | Group   | Filter[] | All provided filters must match. Must specify at least one filter.    |
| `or`     | Group   | Filter[] | One or more of provided filter must match.                            |
| `not`    | Meta    | Filter   | Inverts the result of the provided Filter.                            |
| `empty`  | Meta    | String   | If the value on the specified path is `null`.                         |
| `exists` | Meta    | String   | If the value on the specified path is present.                        |
| `equals` | [Equals](#equals) || Checks if the element-value and the filter-value are the same.        |
| `range`  | [Range](#range)   || Checks if the element-value is in the range of the filter-range.      |
| `in`     | [In](#in)         || Checks if the element-value and any of the filter-values are the same.|

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
            "path": "fields.myField",
            "value": 1
          }
        },
        {
          "in": {
            "path": "fields.otherField",
            "values": ["hello", "world"]
          }
        }
      ]
    }
  ]
}
```

 ### Equals

Checks if the element-value and the filter-value are the same.

| Property    | Type     | Required | Description                                                                            |
| ----------- | -------- | -------- | -------------------------------------------------------------------------------------- |
| `path`      | String   | Required | Path to the element-value.                                                             |
| `value`     | Anything | Optional | The filter-value to check against. Defaults to `null`.                                 |
| `valuePath` | String   | Optional | The path to the other value to compare against. May only be set if `value` is not set! |

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

### In

Checks if any element-value equals one of the filter-values.
If the value on `path` is an array, it'll check if there's at least one element in the cross-section.
To fail on these arrays, use the `strict` option.

| Property     | Type         | Required | Description                                                                                      |
| ------------ | ------------ | -------- | ------------------------------------------------------------------------------------------------ |
| `path`       | String       | Required | Path to the element-value(s).                                                                    |
| `values`     | Array        | Optional | An array of any value which is compared against the element-value.                               |
| `valuesPath` | String       | Optional | Path to the values to check against.                                                             |
| `strict`     | Boolean      | Optional | If the filter should fail when the `path` does not point to a single value. Defaults to `false`. |

## Misc Configurations

Formatters like for Dates (`dateFormat`, `dateLocale`, and `dateZone`) or Numbers (`numberLocale`, `numberStyle`, `numberCurrency`, `numberFractionMinimum`, and `numberFractionMaximum`) or inline mappings like `booleanMapping` are defaulted on default.
You can then override these defaults for the request by overriding them on the root of the request, or go even more detailed by providing it in the mapping option for a column.

Override Hierachy:
* Default Value
* Request Value
* Mapping Value

### Boolean Mapping

With this option you can configure the output of boolean values.
The mapping is a simple object like this:

```json
{
  "true": "yes",
  "false": "no",
  "null": "unknown"
}
```

On default, the mapping is set to the following:
* `true`: `"1"`
* `false` and `null`: `"0"`
