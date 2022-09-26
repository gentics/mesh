:icons: font

////
* Write changelog entries in present tense
* Include GitHub issue or PR if possible using link:http://...[#123] format
* Review your changelog entries
* Don't include security sensitive information in the changelog
* Include links to documentation if possible
////

= Mesh Commercial Plugin Commons Changelog

[[v1.6.26]]
== 1.6.26 (TBD)

CAUTION: Plugins require Mesh >= 1.6.34 to run.

icon:check[] Common: The dependency to Mesh has been updated to 1.6.34 and various possible connection leaks have been fixed.

[[v1.6.19]]
== 1.6.19 (2022-02-14)

icon:plus[] Common: Adds the `RenderService` which wraps Handlebars template rendering and makes sure that all template names start with the correct base template bath. The service can be injected automatically in which case the templates have to reside in _STORAGE_DIR/templates/_, or it can be created with an explicit base path.

[[v1.6.14]]
== 1.6.14 (2021-09-20)

icon:check[] Common: The `SslContextUtil` class now offers more fine-grained control over SSL context generation by exposing intermediary methods.

[[v1.6.3]]
== 1.6.3 (2021-04-08)

icon:plus[] New Forms Plugin

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
