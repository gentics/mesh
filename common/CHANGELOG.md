:icons: font

////
* Write changelog entries in present tense
* Include GitHub issue or PR if possible using link:http://...[#123] format
* Review your changelog entries
* Don't include security sensitive information in the changelog
* Include links to documentation if possible
////

= Mesh Commercial Plugin Commons Changelog

[[v1.4.910]
== 1.4.10 (2020-07-14)

icon:check[] Common: Error handling in handlers created by `HandlerUtil` will now optionally send a response with the respective status code and no body, or a JSON representation of the error.

[[v1.4.9]]
== 1.4.9 (2020-07-02)

icon:check[] MeshPluginService: Make sure project initialization via `prepareProjects()` does not initialize multiple projects in parallel.

icon:check[] MeshPluginService: Fix Vert.x event bus usage which caused problems during initialization in clustered setups.

[[v1.3.12]]
== 1.3.12 (2020-07-14)

icon:check[] Common: Error handling in handlers created by `HandlerUtil` will now optionally send a response with the respective status code and no body, or a JSON representation of the error.

[[v1.3.11]]
== 1.3.11 (2020-07-02)

icon:check[] MeshPluginService: Make sure project initialization via `prepareProjects()` does not initialize multiple projects in parallel.

icon:check[] MeshPluginService: Fix Vert.x event bus usage which caused problems during initialization in clustered setups.
