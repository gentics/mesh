:icons: font

////
* Write changelog entries in present tense
* Include GitHub issue or PR if possible using link:http://...[#123] format
* Review your changelog entries
* Don't include security sensitive information in the changelog
* Include links to documentation if possible
////

= Mesh Search Plugin Changelog

[[v1.4.6]]
== 1.4.6 (2020-05-08)
icon:check[] Custom Search: Made it possible to define a seperate pageSize for each custom Query.
icon:check[] All routes now require the request header `Accept: application/json` instead of `Accept: application/json; charset=utf8`. This makes it possible to use the search plugin with the Java MeshRestClient.

[[v1.4.4]]
== 1.4.4 (2020-04-02)
icon:check[] AutocompleteHandler: Fix a bug which caused all custom searches to be treated as normal searches instead of basing it on type.

[[v1.0.1]]
== 1.0.1 (2019-11-08)

CAUTION: In future versions of the plugin the returned total hits might not be exact but a lower bound.

icon:check[] SearchHandler: Fix compatibility issues with Elasticsearch 7.

[[v1.0.0]]
== 1.0.0 (2019-08-20)

Initial release


