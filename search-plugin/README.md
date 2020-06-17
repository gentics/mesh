# Search Plugin

For most search use cases, adjusting the plugin configuration and search
queries should be enough. So no extra Java code and build process is required.

(Maybe the `BasicHitTransformer` needs some adjustments for this to be
entirely true.)

This README covers only very basic usage of the plugin. For a more indepth
description, see the documentation in _search.adoc_.

## Endpoints

The plugin only registers project specific endpoints.

* `/api/v1/[project]/plugins/search/search`: normal search
* `/api/v1/[project]/plugins/search/autocomplete`: autocompletion

## Configuration

The plugin is currently aware of the following configuration in the
_config.yml_:

* `projects`: defines for each project, which other projects will be searched.
Note that every project will always search "itself" even if it is not explicitly
specified. For example, the configuration:
```
projects:
  foo:
    - foo
    - bar
  bar:
    - doo
```

means that searches requests to the endpoint of project `foo` will return
results from the projects `foo` and `bar`, requests to the endpoint of
project `bar` will return results from `bar` (implicitly) and `doo`. And
requests to the endpoint for project `doo` will return only results from
`doo`.

* `resultQuery`: the name of the GraphQL query (without extension) to load
search result details. When this is empty, no GraphQL query will be used, but
the hits from Elasticsearch are passed directly to the `HitTransformer`.

* `didYouMeanThreshold`: when fewer that `didYouMeanThreshold` hits are found,
the did-you-mean queries will be executed to suggest another search term.

* `expireTime`: how many seconds certain information (about schemas and
branches) is cached.

## Queries

The `TemplateQueryBuilder` will take the loaded query (as a String) and
replace the Handlebars placeholders

* `query`
* `lang`
* `tags`, and
* `projects`

which are taken from the requests parameters and the configuration.

## HitTransformer

The fields `branch`, `uuid`, `language` and `url` are automatically added for
each search result. The rest is inserted by the injected `HitTransformer`.

The default `BasicHitTransformer` should copy all entries from
`node.elements.languages.fields` to the result.

## TODOs

* Add integration test once the new plugin API is released.
