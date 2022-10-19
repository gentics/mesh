:icons: font

////
* Write changelog entries in present tense
* Include GitHub issue or PR if possible using link:http://...[#123] format
* Review your changelog entries
* Don't include security sensitive information in the changelog
* Include links to documentation if possible
////

= Mesh Search Plugin Changelog

[[v1.6.27]]
== 1.6.27 (2022-10-19)

icon:check[] HitTransformer: Fixes excerpt generation in `BasicHitTransformer`.

icon:plus[] HitTransformer: Adds raw highlights to the search result in addition to the excerpt. The highlights are grouped by fieldname, and the contents is determined by the `fragment_size` and `number_of_fragments` settings in the query (see: [Elasticsearch documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/highlighting.html#highlighting-settings)).

[[v1.6.26]]
== 1.6.26 (2022-09-28)

CAUTION: The plugin requires Mesh >= 1.6.34 to run.

icon:check[] The dependency to Mesh has been updated to 1.6.34 and various possible connection leaks have been fixed.

icon:check[] Custom queries: when a custom query specified a specific page size, the `hasMore` field was incorrectly
taking into account the global `pageSize`. This has been fixed.

[[v1.6.13]]
== 1.6.13 (2020-09-22)

icon:plus[] Configuration: Add `waitForIdle` setting which determines whether the `waitForIdle` parameter should be
added for search requests. When the setting is `null` the parameter will not be added, otherwise it is added with
the configured value.

[[v1.5.8]]
== 1.5.8 (2020-09-23)

icon:plus[] SearchHandler: Add detection of global aggregations.

icon:check[] Custom queries: Add page parameter `p` for custom queries.

icon:check[] QueryBuilder: The tags parameter `t` is now injected in all query types.

[[v1.5.7]]
== 1.5.7 (2020-08-24)

icon:plus[] BasicHitTransformer: add all information from the GraphQl Search results

[[v1.5.5]]
== 1.5.5 (2020-07-23)

icon:check[] The `t` parameter can now also be used in custom queries in order to specify the `tags` render context parameter for handlebars templates.

[[v1.5.4]]
== 1.5.4 (2020-07-17)

icon:check[] SearchHandler: Fix branch UUID detection for language specific indices introduced by Mesh 1.5.0.

[[v1.5.1]]
== 1.5.1 (2020-06-05)

icon:check[] Added JSON Content-type headers to the search response in the Custom search handler

[[v1.4.13]]
== 1.4.13 (2020-09-23)

icon:plus[] SearchHandler: Add detection of global aggregations.

icon:check[] Custom queries: Add page parameter `p` for custom queries.

icon:check[] QueryBuilder: The tags parameter `t` is now injected in all query types.

[[v1.4.12]]
== 1.4.12 (2020-08-24)

icon:plus[] BasicHitTransformer: add all information from the GraphQl Search results

[[v1.4.8]]
== 1.4.8 (2020-06-05)

icon:check[] Added JSON Content-type headers to the search response in the Custom search handler

[[v1.4.6]]
== 1.4.6 (2020-05-08)

icon:check[] Custom Search: Made it possible to define a seperate pageSize for each custom Query.
icon:check[] All routes now require the request header `Accept: application/json` instead of `Accept: application/json; charset=utf8`. This makes it possible to use the search plugin with the Java MeshRestClient.

[[v1.4.4]]
== 1.4.4 (2020-04-02)

icon:check[] AutocompleteHandler: Fix a bug which caused all custom searches to be treated as normal searches instead of basing it on type.

[[v1.3.10]]
== 1.3.10 (2020-06-05)

icon:check[] Added JSON Content-type headers to the search response in the Custom search handler

[[v1.1.1]]
== 1.1.1 (2020-06-05)

icon:check[] Added JSON Content-type headers to the search response in the Custom search handler

[[v1.0.1]]
== 1.0.1 (2019-11-08)

CAUTION: In future versions of the plugin the returned total hits might not be exact but a lower bound.

icon:check[] SearchHandler: Fix compatibility issues with Elasticsearch 7.

[[v1.0.0]]
== 1.0.0 (2019-08-20)

Initial release
