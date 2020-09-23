:icons: font

////
* Write changelog entries in present tense
* Include GitHub issue or PR if possible using link:http://...[#123] format
* Review your changelog entries
* Don't include security sensitive information in the changelog
* Include links to documentation if possible
////

= Mesh Search Plugin Changelog

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
