:icons: font

////
* Write changelog entries in present tense
* Include GitHub issue or PR if possible using link:http://...[#123] format
* Review your changelog entries
* Don't include security sensitive information in the changelog
* Include links to documentation if possible
////

= Mesh Commercial Plugin Commons Changelog

[[v1.6.2]]
== 1.6.2 (2020-10-01)

icon:check[] Common: The `HandlerUtil#meshBinaryHandler()` uses the `verboseErrorHandler` now also for errors during download

[[v1.6.1]
== 1.6.1 (2020-00-14)

CAUTION: The method `Miscutils.loadResource()` method is deprecated. Use
`MiscUtils.loadResourceString()` instead.

icon:plus[] Common: Add methods `loadResourceString()`, `loadResourceBytes()`
and `loadResourceStream()` to load resources to `MiscUtils`.

[[v1.5.10]]
== 1.5.10 (2020-10-01)

icon:check[] Common: The `HandlerUtil#meshBinaryHandler()` uses the `verboseErrorHandler` now also for errors during download

[[v1.5.9]
== 1.5.9 (2020-00-14)

CAUTION: The method `Miscutils.loadResource()` method is deprecated. Use
`MiscUtils.loadResourceString()` instead.

icon:plus[] Common: Add methods `loadResourceString()`, `loadResourceBytes()`
and `loadResourceStream()` to load resources to `MiscUtils`.

[[v1.5.3]]
== 1.5.3 (2020-07-14)

icon:check[] Common: Error handling in handlers created by `HandlerUtil` will now optionally send a response with the respective status code and no body, or a JSON representation of the error.

[[v1.5.2]]
== 1.5.2 (2020-07-02)

icon:check[] MeshPluginService: Make sure project initialization via `prepareProjects()` does not initialize multiple projects in parallel.

icon:check[] MeshPluginService: Fix Vert.x event bus usage which caused problems during initialization in clustered setups.

[[v1.4.15]]
== 1.4.15 (2020-10-01)

icon:check[] Common: The `HandlerUtil#meshBinaryHandler()` uses the `verboseErrorHandler` now also for errors during download

[[v1.4.14]
== 1.4.14 (2020-09-14)

CAUTION: The method `Miscutils.loadResource()` method is deprecated. Use
`MiscUtils.loadResourceString()` instead.

icon:plus[] Common: Add methods `loadResourceString()`, `loadResourceBytes()`
and `loadResourceStream()` to load resources to `MiscUtils`.

[[v1.4.10]
== 1.4.10 (2020-07-14)

icon:check[] Common: Error handling in handlers created by `HandlerUtil` will now optionally send a response with the respective status code and no body, or a JSON representation of the error.

[[v1.4.9]]
== 1.4.9 (2020-07-02)

icon:check[] MeshPluginService: Make sure project initialization via `prepareProjects()` does not initialize multiple projects in parallel.

icon:check[] MeshPluginService: Fix Vert.x event bus usage which caused problems during initialization in clustered setups.

[[v1.3.13]]
== 1.3.13 (2020-10-01)

icon:check[] Common: The `HandlerUtil#meshBinaryHandler()` uses the `verboseErrorHandler` now also for errors during download

[[v1.3.12]]
== 1.3.12 (2020-07-14)

icon:check[] Common: Error handling in handlers created by `HandlerUtil` will now optionally send a response with the respective status code and no body, or a JSON representation of the error.

[[v1.3.11]]
== 1.3.11 (2020-07-02)

icon:check[] MeshPluginService: Make sure project initialization via `prepareProjects()` does not initialize multiple projects in parallel.

icon:check[] MeshPluginService: Fix Vert.x event bus usage which caused problems during initialization in clustered setups.
