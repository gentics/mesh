# Proposal for Favorite Plugin

## Functional Description

* The plugin stores user-specific lists of strings (favorites) for types (favorite-type)
* Anonymous calls are not allowed
* favorite-types are configured in the `config.yml` (cross project)
* All values can be read for favorite-type
* Single value can be added to favorite-type
* Single value can be deleted from favorite-type
* All values can be overwritten for favorite-type

## REST API

### Get Favorits
```http
GET /api/v1/[project]/plugins/favorite/[type]
```

Returns
```json
{
    "favorites": [
        "my_widget3",
        "my_widget2"
    ]
}
```

### Add value to favorites
```http
POST /api/v1/[project]/plugins/favorite/[type]/[value]
```

### Remove value from favorites
```http
DELETE /api/v1/[project]/plugins/favorite/[type]/[value]
```

### Overwrite favorite values
```http
POST /api/v1/[project]/plugins/favorite/[type]
{
    "favorites": [
        "my_widget3",
        "my_widget2"
    ]
}
```

## Configuration

```yml
projects:
  - "project_1"
  - "project_2"
types:
    widgets:
        maxEntries: -1
        allowDuplicates: false
        reorderDuplicates: false
    infobox:
        maxEntries: -1
        allowDuplicates: false
        reorderDuplicates: false
    ongoing_procedures:
        maxEntries: 3
        allowDuplicates: false
        reorderDuplicates: true
```

| Key | Type | Description | Default value |
|-----|------|-------------|---------------|
| projects   | array of strings | Project names, for which the plugin is activated | REQUIRED |
| types      | array of objects | Available types with their configuration | REQUIRED |
| maxEntries | integer | Maximum number of entries stored for the type. -1 for unbounded | -1 |
| allowDuplicates   | boolean          | Flag to allow duplicate values for the type | false |
| reorderDuplicates | boolean          | Flag to specify behaviour for POSTed duplicates, if `allowDuplicates` is `false`. If set to `false`, duplicates will just be ignored, if `true` they will be reordered to the back of the list | false |
